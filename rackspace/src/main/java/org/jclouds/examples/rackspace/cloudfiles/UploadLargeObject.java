/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.examples.rackspace.cloudfiles;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;

import java.io.Closeable;
import java.io.File;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.jclouds.blobstore.options.PutOptions.Builder.multipart;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.CONTAINER;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.PROVIDER;

/**
 * Upload a large object in the Cloud Files container from the CreateContainer example.
 *  
 * @author Everett Toews
 */
public class UploadLargeObject implements Closeable {
   private BlobStore blobStore;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    * The third argument (args[2]) must be the absolute path to a large file
    */
   public static void main(String[] args) {
      UploadLargeObject createContainer = new UploadLargeObject(args[0], args[1]);

      try {
         createContainer.uploadLargeObjectFromFile(new File(args[2]));
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createContainer.close();
      }
   }

   public UploadLargeObject(String username, String apiKey) {
      Properties overrides = new Properties();
      // This property controls the number of parts being uploaded in parallel, the default is 4
      overrides.setProperty("jclouds.mpu.parallel.degree", "5");
      // This property controls the size (in bytes) of parts being uploaded in parallel, the default is 33554432 bytes = 32 MB
      overrides.setProperty("jclouds.mpu.parts.size", "67108864"); // 64 MB

      BlobStoreContext context = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .overrides(overrides)
            .buildView(BlobStoreContext.class);
      blobStore = context.getBlobStore();
   }

   /**
    * Upload a large object from a File using the Swift API. 
    * @throws ExecutionException 
    * @throws InterruptedException 
    */
   private void uploadLargeObjectFromFile(File largeFile) throws InterruptedException, ExecutionException {
      System.out.format("Upload Large Object From File%n");

      Blob blob = blobStore.blobBuilder(largeFile.getName())
            .payload(largeFile)
            .build();
      
      String eTag = blobStore.putBlob(CONTAINER, blob, multipart());

      System.out.format("  Uploaded %s eTag=%s", largeFile.getName(), eTag);
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      if (blobStore != null) {
         blobStore.getContext().close();
      }
   }
}
