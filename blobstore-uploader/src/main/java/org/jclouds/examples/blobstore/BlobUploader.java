/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.examples.blobstore;

import static com.google.common.collect.Iterables.getOnlyElement;

import java.io.File;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.domain.Location;
import org.jclouds.io.payloads.ByteSourcePayload;
import org.jclouds.openstack.swift.v1.blobstore.RegionScopedBlobStoreContext;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;

public class BlobUploader implements Runnable {
   /**
    * ThreadLocal allows us to use 1 container and 1 connection per thread and reuse them.
    * They need to be static, but here we also have to instantiate them at runtime, as we pass parameters from
    * the command line.
    */
   private static ThreadLocal<BlobStore> blobStore = new ThreadLocal<BlobStore>();
   private static ThreadLocal<String> container = new ThreadLocal<String>();

   /**
    * The only thing that really needs to be passed to this unit of work is the File to be uploaded.
    * The credentials are here for convenience.
    */
   private String username;
   private String password;
   private String provider;
   private String region;
   private File file;

   public
   BlobUploader(String username, String password, String provider, String region, File file) {
      this.username = username;
      this.password = password;
      this.provider = provider;
      this.region = region;
      this.file = file;
   }

   @Override
   public void run() {
      /**
       * Instantiate the ThreadLocal variables when this thread runs for the first time.
       * Instantiating this in the constructor will not work (different thread).
       */
      if (blobStore.get() == null) {
         // It is usually a good idea to include the currentThread when logging parallel tasks.
         System.out.println("Creating connection for thread " + Thread.currentThread());
         /**
          * In some cases, especially when running very large jobs with many parallel threads, some connections will
          * break. In that case, we need to be able to obtain a new connection (and socket) to the service, which is
          * why this is factored out.
          */
         resetBlobstore(username, password, provider, region);
      }

      if (container.get() == null) {
         container.set(UUID.randomUUID().toString());
         Location location = getOnlyElement(blobStore.get().listAssignableLocations());
         blobStore.get().createContainerInLocation(location, container.get());

         System.out.println("Created container " + container.get() +
               " for thread " + Thread.currentThread() +
               " in " + location.toString());
      }

      // The md5 as returned by the service, and as calculated locally.
      String md5Local;
      String md5Remote;
      Blob blob;

      try {
         md5Local = BaseEncoding.base16().encode(Files.hash(file, Hashing.md5()).asBytes()).toLowerCase();
      } catch (java.io.IOException e) {
         e.printStackTrace();
         /**
          * The file is no longer available on the local FS.
          * In some application cases, you might also want to retry this instead of finishing the unit of work.
          */
         return;
      }

      ByteSourcePayload bsp = new ByteSourcePayload(Files.asByteSource(file));

      /**
       * Uploading a file over a network is an inherently fragile operation. Over thousands of files, especially in
       * highly parallel jobs that tax upload bandwidth, a small percent of uploads are guaranteed to fail.
       */
      do {
         System.out.println("Uploading " + file.getName() + " ; " + FileUtils.sizeOf(file));
         blob = blobStore.get().blobBuilder(file.getName())
                  .payload(bsp)
                  .build();
         md5Remote = blobStore.get().putBlob(container.get(), blob).toLowerCase();
         if (md5Local.equals(md5Remote)) {
            long total = BlobUploaderMain.bytesUploaded.addAndGet(FileUtils.sizeOf(file));
            System.out.println("Uploaded MB: " + (int)total / FileUtils.ONE_MB + "MB ; " + (int)((float)BlobUploaderMain.bytesUploaded.get() / BlobUploaderMain.totalBytes) * 100 + "%");
            bsp.release();
            return;
         } else {
            System.out.printf("md5 mismatch %s vs %s, retrying %s", md5Local, md5Remote, file.getName());
         }
      } while(true);
   }

   private void resetBlobstore(String username, String password, String provider, String region) {
      Properties overrides = new Properties();
      // Retry after 25 seconds of no response
      overrides.setProperty(Constants.PROPERTY_SO_TIMEOUT, "25000");
      // Keep retrying indefinitely
      overrides.setProperty(Constants.PROPERTY_MAX_RETRIES, String.valueOf(Integer.MAX_VALUE));
      // Do not wait between retries
      overrides.setProperty(Constants.PROPERTY_RETRY_DELAY_START , "0");

      ContextBuilder builder = ContextBuilder.newBuilder(provider)
            .overrides(overrides)
            .credentials(username, password);
      blobStore.set(builder.buildView(RegionScopedBlobStoreContext.class).getBlobStore(region));
   }
}
