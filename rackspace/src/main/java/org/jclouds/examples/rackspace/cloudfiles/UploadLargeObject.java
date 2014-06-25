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

import static org.jclouds.blobstore.options.PutOptions.Builder.multipart;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.CONTAINER;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.REGION;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.jclouds.openstack.swift.v1.blobstore.RegionScopedBlobStoreContext;

import com.google.common.io.ByteSource;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

/**
 * Upload a large object in the Cloud Files container from the CreateContainer example.
 */
public class UploadLargeObject implements Closeable {
   private BlobStore blobStore;

   /**
    * To get a username and API key see http://jclouds.apache.org/guides/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    * The third argument (args[2]) must be the absolute path to a large file
    */
   public static void main(String[] args) throws IOException {
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

      RegionScopedBlobStoreContext context = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .overrides(overrides)
            .buildView(RegionScopedBlobStoreContext.class);
      blobStore = context.blobStoreInRegion(REGION);
   }

   /**
    * Upload a large object from a File using the BlobStore API.
    *
    * @throws ExecutionException
    * @throws InterruptedException
    */
   private void uploadLargeObjectFromFile(File largeFile) throws InterruptedException, ExecutionException {
      System.out.format("Upload Large Object From File%n");

      ByteSource source = Files.asByteSource(largeFile);
      // create the payload and set the content length
      Payload payload = Payloads.newByteSourcePayload(source);
      payload.getContentMetadata().setContentLength(largeFile.length());

      Blob blob = blobStore.blobBuilder(largeFile.getName())
            .payload(payload)
            .build();

      // configure the blobstore to use multipart uploading of the file
      String eTag = blobStore.putBlob(CONTAINER, blob, multipart());

      System.out.format("  Uploaded %s eTag=%s", largeFile.getName(), eTag);
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(blobStore.getContext(), true);
   }
}
