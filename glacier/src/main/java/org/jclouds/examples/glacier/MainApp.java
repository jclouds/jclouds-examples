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
package org.jclouds.examples.glacier;

import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.UUID;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.options.PutOptions;
import org.jclouds.glacier.GlacierClient;
import org.jclouds.glacier.blobstore.strategy.internal.BasePollingStrategy;
import org.jclouds.glacier.domain.ArchiveRetrievalJobRequest;
import org.jclouds.glacier.domain.JobRequest;
import org.jclouds.io.ByteSources;
import org.jclouds.io.Payload;
import org.jclouds.io.payloads.ByteSourcePayload;
import org.jclouds.util.Strings2;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.ByteSource;
import com.google.common.io.CharStreams;

/**
 * Demonstrates the use of Glacier provider and BlobStore.
 *
 * Usage is: java MainApp "identity" "credential"
 */
public class MainApp {
   private static final long MiB = 1L << 20;

   public static void main(String[] args) throws IOException {
      if (args.length < 2) {
         throw new IllegalArgumentException("Invalid number of parameters. Syntax is: \"identity\" \"credential\"");
      }

      String identity = args[0];
      String credentials = args[1];

      // Init
      BlobStoreContext context = ContextBuilder.newBuilder("glacier")
            .credentials(identity, credentials)
            .buildView(BlobStoreContext.class);

      try {
         while (chooseOption(context));
      } finally {
         context.close();
      }
   }

   private static void putAndRetrieveBlobExample(BlobStore blobstore) throws IOException {
      // Create a container
      String containerName = "jclouds_putAndRetrieveBlobExample_" + UUID.randomUUID().toString();
      blobstore.createContainerInLocation(null, containerName); // Create a vault

      // Create a blob
      ByteSource payload = ByteSource.wrap("data".getBytes(Charsets.UTF_8));
      Blob blob = blobstore.blobBuilder("ignored") // The blob name is ignored in Glacier
            .payload(payload)
            .contentLength(payload.size())
            .build();

      // Put the blob in the container
      String blobId = blobstore.putBlob(containerName, blob);

      // Retrieve the blob
      Blob result = blobstore.getBlob(containerName, blobId);

      // Print the result
      InputStream is = result.getPayload().openStream();
      try {
         String data = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
         System.out.println("The retrieved payload is: " + data);
      } finally {
         is.close();
      }
   }

   private static void multipartUploadExample(BlobStore blobstore) throws IOException {
      // Create a container
      String containerName = "jclouds_multipartUploadExample_" + UUID.randomUUID().toString();
      blobstore.createContainerInLocation(null, containerName); // Create a vault

      // Create a blob
      ByteSource payload = buildData(16 * MiB);
      Blob blob = blobstore.blobBuilder("ignored") // The blob name is ignored in Glacier
            .payload(payload)
            .contentLength(payload.size())
            .build();

      // Create the PutOptions
      PutOptions options = PutOptions.Builder.multipart(true);

      // Put the blob in the container
      blobstore.putBlob(containerName, blob, options);
      System.out.println("The blob has been uploaded");
   }

   private static void interruptionExample(final BlobStore blobstore) throws IOException {
      // Create a container
      final String containerName =  "jclouds_interruptionExample_" + UUID.randomUUID().toString();
      blobstore.createContainerInLocation(null, containerName); // Create a vault

      // Create a blob
      ByteSource payload = ByteSource.wrap("data".getBytes(Charsets.UTF_8));
      Blob blob = blobstore.blobBuilder("ignored") // The blob name is ignored in Glacier
            .payload(payload)
            .contentLength(payload.size())
            .build();

      // Put the blob in the container
      final String blobId = blobstore.putBlob(containerName, blob);

      // New thread
      Thread thread = new Thread() {
         public void run() {
            try {
               blobstore.getBlob(containerName, blobId);
            } catch (RuntimeException e) {
               System.out.println("The request was aborted");
            }
         }
      };

      // Start and interrupt the thread
      thread.start();
      thread.interrupt();
      try {
         thread.join();
      } catch (InterruptedException e) {
         Throwables.propagate(e);
      }
   }

   private static void providerExample(BlobStoreContext context) throws IOException {
      // Get the provider API
      GlacierClient client = context.unwrapApi(GlacierClient.class);

      // Create a vault
      final String vaultName =  "jclouds_providerExample_" + UUID.randomUUID().toString();
      client.createVault(vaultName);

      // Upload an archive
      Payload payload = new ByteSourcePayload(buildData(16));
      payload.getContentMetadata().setContentType(PLAIN_TEXT_UTF_8.toString());
      payload.getContentMetadata().setContentLength(16L);
      String archiveId = client.uploadArchive(vaultName, payload);

      // Create an archive retrieval job request
      JobRequest archiveRetrievalJobRequest = ArchiveRetrievalJobRequest.builder()
            .archiveId(archiveId)
            .description("retrieval job")
            .build();

      // Initiate job
      String jobId = client.initiateJob(vaultName, archiveRetrievalJobRequest);
      try {
         // Poll until the job is done
         new BasePollingStrategy(client).waitForSuccess(vaultName, jobId);

         // Get the job output
         Payload result = client.getJobOutput(vaultName, jobId);

         // Print the result
         System.out.println("The retrieved payload is: " + Strings2.toStringAndClose(result.openStream()));
      } catch (InterruptedException e) {
         Throwables.propagate(e);
      }
   }

   public static boolean chooseOption(BlobStoreContext context) throws IOException {
      Scanner scan = new Scanner(System.in);
      System.out.println("");
      System.out.println("Glacier examples");
      System.out.println("1. Put and retrieve blob (~4-5 hours)");
      System.out.println("2. Multipart upload (~1-5 minutes)");
      System.out.println("3. Call interruption (~0-2 minutes)");
      System.out.println("4. Provider API (~4-5 hours)");
      System.out.println("5. Exit");
      System.out.print("Choose an option: ");
      try{
         switch(scan.nextInt()){
         case 1:
            putAndRetrieveBlobExample(context.getBlobStore());
            break;
         case 2:
            multipartUploadExample(context.getBlobStore());
            break;
         case 3:
            interruptionExample(context.getBlobStore());
            break;
         case 4:
            providerExample(context);
            break;
         case 5:
            return false;
         default:
            System.out.println("Not a valid option");
            break;
         }
      }
      catch(InputMismatchException e) {
         System.out.println("Not a valid option");
      }
      return true;
   }

   private static ByteSource buildData(long size) {
      byte[] array = new byte[1024];
      Arrays.fill(array, (byte) 'a');
      return ByteSources.repeatingArrayByteSource(array).slice(0, size);
   }
}
