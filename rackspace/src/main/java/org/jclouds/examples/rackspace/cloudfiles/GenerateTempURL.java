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

import static org.jclouds.examples.rackspace.cloudfiles.Constants.CONTAINER;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.REGION;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpResponse;
import org.jclouds.http.HttpResponseException;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.jclouds.openstack.swift.v1.blobstore.RegionScopedBlobStoreContext;

import com.google.common.io.ByteSource;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

/**
 * The Temporary URL feature (TempURL) allows you to create limited-time Internet addresses which allow you to grant
 * limited access to your Cloud Files account. Using TempURL, you may allow others to retrieve or place objects in
 * your Cloud Files account for as long or as short a time as you wish. Access to the TempURL is independent of
 * whether or not your account is CDN-enabled. And even if you don't CDN-enable a directory, you can still grant
 * temporary public access through a TempURL.
 *
 * This feature is useful if you want to allow a limited audience to download a file from your Cloud Files account or
 * website. You can give out the TempURL and know that after a specified time, no one will be able to access your
 * object through the address. Or, if you want to allow your audience to upload objects into your Cloud Files account,
 * you can give them a TempURL. After the specified time expires, no one will be able to upload to the address.
 *
 * Additionally, you need not worry about time running out when someone downloads a large object. If the time expires
 * while a file is being retrieved, the download will continue until it is finished. Only the link will expire.
 *
 */
public class GenerateTempURL implements Closeable {
   private static final String FILENAME = "object.txt";
   private static final int TEN_MINUTES = 10 * 60;

   private final BlobStore blobStore;
   private final RegionScopedBlobStoreContext blobStoreContext;

   /**
    * To get a username and API key see http://jclouds.apache.org/guides/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      GenerateTempURL generateTempURL = new GenerateTempURL(args[0], args[1]);

      try {
         generateTempURL.createContainer();
         generateTempURL.generatePutTempURL();
         generateTempURL.generateGetTempURL();
         generateTempURL.generateDeleteTempURL();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         generateTempURL.close();
      }
   }

   public GenerateTempURL(String username, String apiKey) {
      blobStoreContext = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildView(RegionScopedBlobStoreContext.class);
      blobStore = blobStoreContext.getBlobStore(REGION);
   }

   private void createContainer() throws IOException {
      // Ensure that the container exists
      Location location = new LocationBuilder().scope(LocationScope.REGION).id(REGION).description("region").build();
      if (!blobStore.containerExists(CONTAINER)) {
            blobStore.createContainerInLocation(location, CONTAINER);
         System.out.format("Created container in %s%n", REGION);
      }
   }

   private void generatePutTempURL() throws IOException {
      System.out.format("Generate PUT Temp URL%n");

      // Create the Payload
      String data = "This object will be public for 10 minutes.";
      ByteSource source = ByteSource.wrap(data.getBytes());
      Payload payload = Payloads.newByteSourcePayload(source);

      // Create the Blob
      Blob blob = blobStore.blobBuilder(FILENAME).payload(payload).contentType("text/plain").build();
      HttpRequest request = blobStoreContext.getSigner(REGION).signPutBlob(CONTAINER, blob, TEN_MINUTES);

      System.out.format("  %s %s%n", request.getMethod(), request.getEndpoint());

      // PUT the file using jclouds
      HttpResponse response = blobStoreContext.utils().http().invoke(request);
      int statusCode = response.getStatusCode();

      if (statusCode >= 200 && statusCode < 299) {
         System.out.format("  PUT Success (%s)%n", statusCode);
      }
      else {
         throw new HttpResponseException(null, response);
      }
   }

   private void generateGetTempURL() throws IOException {
      System.out.format("Generate GET Temp URL%n");

      HttpRequest request = blobStoreContext.getSigner(REGION).signGetBlob(CONTAINER, FILENAME, TEN_MINUTES);

      System.out.format("  %s %s%n", request.getMethod(), request.getEndpoint());

      // GET the file using jclouds
      File file = File.createTempFile(FILENAME, ".tmp");
      Payload payload = blobStoreContext.utils().http().invoke(request).getPayload();

      try {
         Files.asByteSink(file).writeFrom(payload.openStream());

         System.out.format("  GET Success (%s)%n", file.getAbsolutePath());
      } finally {
         payload.release();
         file.delete();
      }
   }

   private void generateDeleteTempURL() throws IOException {
      System.out.format("Generate DELETE Temp URL%n");

      HttpRequest request = blobStoreContext.getSigner(REGION).signRemoveBlob(CONTAINER, FILENAME);

      System.out.format("  %s %s%n", request.getMethod(), request.getEndpoint());

      // DELETE the file using jclouds
      HttpResponse response = blobStoreContext.utils().http().invoke(request);
      int statusCode = response.getStatusCode();

      if (statusCode >= 200 && statusCode < 299) {
         System.out.format("  DELETE Success (%s)%n", statusCode);
      }
      else {
         throw new HttpResponseException(null, response);
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(blobStore.getContext(), true);
   }
}
