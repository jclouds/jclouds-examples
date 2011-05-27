/**
 *
 * Copyright (C) 2010 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.jclouds.examples.blobstore.largeblob;

import static org.jclouds.Constants.PROPERTY_ENDPOINT;
import static org.jclouds.blobstore.options.PutOptions.Builder.multipart;
import static org.jclouds.location.reference.LocationConstants.ENDPOINT;
import static org.jclouds.location.reference.LocationConstants.PROPERTY_REGION;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.MediaType;

import org.jclouds.aws.domain.Region;
import org.jclouds.blobstore.AsyncBlobStore;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.util.BlobStoreUtils;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Module;

/**
 * Demonstrates the use of {@link BlobStore}.
 * 
 * Usage is: java MainApp \"provider\" \"identity\" \"credential\" \"localFileName\"
 * \"containerName\" \"objectName\" plainhttp threadcount
 * 
 * \"plainhttp\" and \"threadcound\" is optional if all the rest of parameters are omitted
 * 
 * @author Tibor Kiss
 * @author Adrian Cole
 */
public class MainApp {

   public static int PARAMETERS = 6;
   public static String INVALID_SYNTAX = "Invalid number of parameters. Syntax is: \"provider\" \"identity\" \"credential\" \"localFileName\" \"containerName\" \"objectName\" plainhttp threadcount";

   public final static Properties PLAIN_HTTP_ENDPOINTS = new Properties();

   static {
      PLAIN_HTTP_ENDPOINTS.setProperty(PROPERTY_ENDPOINT, "http://s3.amazonaws.com");
      PLAIN_HTTP_ENDPOINTS.setProperty(PROPERTY_REGION + "." + Region.US_STANDARD + "." + ENDPOINT,
               "http://s3.amazonaws.com");
      PLAIN_HTTP_ENDPOINTS.setProperty(PROPERTY_REGION + "." + Region.US_WEST_1 + "." + ENDPOINT,
               "http://s3-us-west-1.amazonaws.com");
      PLAIN_HTTP_ENDPOINTS.setProperty(PROPERTY_REGION + "." + "EU" + "." + ENDPOINT,
               "http://s3-eu-west-1.amazonaws.com");
      PLAIN_HTTP_ENDPOINTS.setProperty(PROPERTY_REGION + "." + Region.AP_SOUTHEAST_1 + "." + ENDPOINT,
               "http://s3-ap-southeast-1.amazonaws.com");
   }

   final static Iterable<? extends Module> NETTY_MODULES = 
      ImmutableSet.of(new JavaUrlHttpCommandExecutorServiceModule(), new Log4JLoggingModule(), new NettyPayloadModule());

   // we may test different http layer with the following
   // ImmutableSet.of(new ApacheHCHttpCommandExecutorServiceModule(), new Log4JLoggingModule(), new
   // NettyPayloadModule());

   static String getSpeed(long speed) {
      if (speed < 1024) {
         return "" + speed + " bytes/s";
      } else if (speed < 1048576) {
         return "" + (speed / 1024) + " kbytes/s";
      } else {
         return "" + (speed / 1048576) + " Mbytes/s";
      }
   }

   static void printSpeed(String message, long start, long length) {
      long sec = (System.currentTimeMillis() - start) / 1000;
      if (sec == 0)
         return;
      long speed = length / sec;
      System.out.print(message);
      if (speed < 1024) {
         System.out.print(" " + length + " bytes");
      } else if (speed < 1048576) {
         System.out.print(" " + (length / 1024) + " kB");
      } else if (speed < 1073741824) {
         System.out.print(" " + (length / 1048576) + " MB");
      } else {
         System.out.print(" " + (length / 1073741824) + " GB");
      }
      System.out.println(" with " + getSpeed(speed) + " (" + length + " bytes)");
   }

   public static void main(String[] args) throws IOException {

      if (args.length < PARAMETERS)
         throw new IllegalArgumentException(INVALID_SYNTAX);

      // Args

      String provider = args[0];
      if (!Iterables.contains(BlobStoreUtils.getSupportedProviders(), provider))
         throw new IllegalArgumentException("provider " + provider + " not in supported list: "
                  + BlobStoreUtils.getSupportedProviders());
      String identity = args[1];
      String credential = args[2];
      String fileName = args[3];
      String containerName = args[4];
      String objectName = args[5];
      boolean plainhttp = args.length >= 7 && "plainhttp".equals(args[6]);
      String threadcount = args.length >= 8 ? args[7] : null;

      // Init
      Properties overrides = new Properties();
      if (plainhttp)
         overrides.putAll(PLAIN_HTTP_ENDPOINTS); // default is https
      if (threadcount != null)
         overrides.setProperty("jclouds.mpu.parallel.degree", threadcount); // without setting,
      // default is 4 threads
      overrides.setProperty(provider + ".identity", identity);
      overrides.setProperty(provider + ".credential", credential);
      BlobStoreContext context = new BlobStoreContextFactory().createContext(provider, NETTY_MODULES, overrides);

      try {
         long start = System.currentTimeMillis();
         // Create Container
         AsyncBlobStore blobStore = context.getAsyncBlobStore(); // it can be changed to sync
         // BlobStore
         blobStore.createContainerInLocation(null, containerName);

         File input = new File(fileName);
         long length = input.length();
         // Add a Blob
         Blob blob = blobStore.blobBuilder(objectName).payload(input)
               .contentType(MediaType.APPLICATION_OCTET_STREAM).contentDisposition(objectName).build();
         // Upload a file
         ListenableFuture<String> futureETag = blobStore.putBlob(containerName, blob, multipart());

         // asynchronously wait for the upload
         String eTag = futureETag.get();

         printSpeed("Sucessfully uploaded eTag(" + eTag + ")", start, length);

      } catch (InterruptedException e) {
         System.err.println(e.getMessage());
         e.printStackTrace();
      } catch (ExecutionException e) {
         System.err.println(e.getMessage());
         e.printStackTrace();
      } finally {
         // Close connecton
         context.close();
         System.exit(0);
      }

   }
}
