/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.examples.rackspace.cloudfiles;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static com.google.common.base.Preconditions.checkArgument;

import java.io.Closeable;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.cloudfiles.CloudFilesApiMetadata;
import org.jclouds.cloudfiles.CloudFilesClient;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Upload an entire directory and all of its sub-directories to a Cloud Files container. The local directory hierarchy
 * will be mimicked as pseudo-hierarchical directories (http://j.mp/rax-hier) within the container. This is a great
 * way to upload content for a static website (http://j.mp/rax-static).     
 * 
 * @author Everett Toews
 */
public class UploadDirectoryToCDN implements Closeable {
   // The provider configures jclouds To use the Rackspace Cloud (US)
   // To use the Rackspace Cloud (UK) set the system property or default value to "cloudfiles-uk"
   private static final String PROVIDER = System.getProperty("cloudfiles.provider", "cloudfiles-us");
   private static final int THREADS = Integer.getInteger("upload.threadpool.size", 10);

   private final BlobStore storage;
   private final CloudFilesClient rackspace;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    * The third argument (args[2]) must be the path to the local directory
    * The fourth argument (args[3]) must be the remote container name
    */
   public static void main(String[] args) {
      UploadDirectoryToCDN uploadDirToCDN = new UploadDirectoryToCDN(args[0], args[1]);

      try {
         uploadDirToCDN.uploadDirectory(args[2], args[3]);
         uploadDirToCDN.enableCdnContainer(args[3]);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         uploadDirToCDN.close();
      }
   }

   public UploadDirectoryToCDN(String username, String apiKey) {
      BlobStoreContext context = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildView(BlobStoreContext.class);
      storage = context.getBlobStore();
      // can use context.unwrapApi(CloudFilesClient.class) in jclouds 1.7
      rackspace = context.unwrap(CloudFilesApiMetadata.CONTEXT_TOKEN).getApi();
   }

   /**
    * Generate a list of all of the local files under the specified dirPath and then upload them to container.
    */
   private void uploadDirectory(String dirPath, String container) throws InterruptedException, ExecutionException {
      File dir = new File(dirPath);
      checkArgument(dir.isDirectory(), "%s is not a directory", dirPath);
      
      System.out.println("Uploading " + dirPath + " to " + container);

      storage.createContainerInLocation(null, container);

      List<BlobDetail> blobDetails = Lists.newArrayList();
      generateFileList(dir, "", blobDetails);
      uploadFiles(container, blobDetails);
   }

   /**
    * Recursively generate the list of files to upload.
    */
   private void generateFileList(File localDir, String remotePath, List<BlobDetail> blobDetails) {
      for (File localFile: localDir.listFiles()) {
         String remoteBlobName = remotePath + localFile.getName();

         if (localFile.isFile()) {
            blobDetails.add(new BlobDetail(remoteBlobName, localFile));
         }
         else {
            generateFileList(localFile, remoteBlobName + "/", blobDetails);
         }
      }
   }

   /**
    * Upload the files in parallel.
    */
   private void uploadFiles(String container, List<BlobDetail> blobDetails)
         throws InterruptedException, ExecutionException {
      ListeningExecutorService executor = MoreExecutors.listeningDecorator(newFixedThreadPool(THREADS));
      List<ListenableFuture<BlobDetail>> blobUploaderFutures = Lists.newArrayList();      
      BlobUploaderCallback blobUploaderCallback = new BlobUploaderCallback();

      try {

         for (BlobDetail blobDetail: blobDetails) {
            BlobUploader blobUploader = new BlobUploader(container, blobDetail);         
            ListenableFuture<BlobDetail> blobDetailFuture = executor.submit(blobUploader);
            blobUploaderFutures.add(blobDetailFuture);

            Futures.addCallback(blobDetailFuture, blobUploaderCallback);
         }

         ListenableFuture<List<BlobDetail>> future = Futures.successfulAsList(blobUploaderFutures);
         List<BlobDetail> uploadedBlobDetails = future.get(); // begin the upload
         
         System.out.println();

         for (int i = 0; i < uploadedBlobDetails.size(); i++) {
            if (uploadedBlobDetails.get(i) != null) {
               BlobDetail blobDetail = uploadedBlobDetails.get(i);
               System.out.format("  %s (eTag: %s)%n", blobDetail.getRemoteBlobName(), blobDetail.getETag());
            }
            else {
               System.out.format(" %s (ERROR)%n", blobDetails.get(i).getLocalFile().getAbsolutePath());
            }
         }
      }
      finally {
         executor.shutdown();
      }
   }

   /**
    * This method will put your container on a Content Distribution Network and
    * make it available as a static website.
    */
   private void enableCdnContainer(String container) {
      System.out.println("Enable CDN");

      URI cdnURI = rackspace.enableCDN(container);
      rackspace.setCDNStaticWebsiteIndex(container, "index.html");

      System.out.println("  Go to " + cdnURI + "/");
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      if (storage != null) {
         storage.getContext().close();
      }
   }

   /**
    * A Callable responsible for uploading an object to a container. Returns a BlobDetail with the eTag of the
    * uploaded object.
    */
   private class BlobUploader implements Callable<BlobDetail> {
      private final String container;
      private final BlobDetail toBeUploadedBlobDetail;
      
      protected BlobUploader(String container, BlobDetail toBeUploadedBlobDetail) {
         this.container = container;
         this.toBeUploadedBlobDetail = toBeUploadedBlobDetail;
      }

      @Override
      public BlobDetail call() throws Exception {
         Blob blob = storage.blobBuilder(toBeUploadedBlobDetail.getRemoteBlobName())
               .payload(toBeUploadedBlobDetail.getLocalFile())
               .contentType("") // allows Cloud Files to determine the content type
               .build();
         String eTag = storage.putBlob(container, blob);
         BlobDetail uploadedBlobDetail = new BlobDetail(
               toBeUploadedBlobDetail.getRemoteBlobName(), toBeUploadedBlobDetail.getLocalFile(), eTag);
         
         return uploadedBlobDetail;
      }
   }

   /**
    * Example of a FutureCallback triggered when an upload has finished. Just prints out a character to inform
    * the user of upload progress.
    */
   private class BlobUploaderCallback implements FutureCallback<BlobDetail> {
      @Override
      public void onSuccess(BlobDetail result) {
         System.out.print(".");
      }

      @Override
      public void onFailure(Throwable t) {
         System.out.print("X " + t);
      }
   }

   /**
    * An immutable class for holding details about an object. When an object has been successfully uploaded the 
    * eTag will be present.
    */
   public static class BlobDetail {
      private final String remoteBlobName;
      private final File localFile;
      private final String eTag;

      protected BlobDetail(String remoteBlobName, File localFile) {
         this(remoteBlobName, localFile, null);
      }

      protected BlobDetail(String remoteBlobName, File localFile, String eTag) {
         this.remoteBlobName = remoteBlobName;
         this.localFile = localFile;
         this.eTag = eTag;
      }

      public String getRemoteBlobName() {
         return remoteBlobName;
      }

      public File getLocalFile() {
         return localFile;
      }

      public String getETag() {
         return eTag;
      }

      public boolean isUploaded() {
         return eTag != null;
      }
   }
}
