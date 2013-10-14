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

import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.cloudfiles.CloudFilesApiMetadata;
import org.jclouds.cloudfiles.CloudFilesClient;
import org.jclouds.openstack.swift.CommonSwiftAsyncClient;
import org.jclouds.openstack.swift.CommonSwiftClient;
import org.jclouds.openstack.swift.domain.SwiftObject;
import org.jclouds.rest.RestContext;

import java.io.*;
import java.net.URI;

import static org.jclouds.examples.rackspace.cloudfiles.Constants.*;

/**
 * This example will create a container, put a file in it, and publish it on the internet!
 */
public class CloudFilesPublish implements Closeable {
   private final BlobStore blobStore;
   private final RestContext<CommonSwiftClient, CommonSwiftAsyncClient> swift;
   private final CloudFilesClient cloudFilesClient;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      CloudFilesPublish cloudFilesPublish = new CloudFilesPublish(args[0], args[1]);

      try {
         cloudFilesPublish.createContainer();
         cloudFilesPublish.createObjectFromFile();
         cloudFilesPublish.enableCdnContainer();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      finally {
         cloudFilesPublish.close();
      }
   }

   public CloudFilesPublish(String username, String apiKey) {
      BlobStoreContext context = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildView(BlobStoreContext.class);
      blobStore = context.getBlobStore();
      swift = context.unwrap();
      cloudFilesClient = context.unwrap(CloudFilesApiMetadata.CONTEXT_TOKEN).getApi();
   }

   /**
    * This method will create a container in Cloud Files where you can store and
    * retrieve any kind of digital asset.
    */
   private void createContainer() {
      System.out.format("Create Container%n");

      swift.getApi().createContainer(CONTAINER_PUBLISH);

      System.out.format("  %s%n", CONTAINER_PUBLISH);
   }

   /**
    * This method will put a plain text object into the container.
    */
   private void createObjectFromFile() throws IOException {
      System.out.format("Create Object From File%n");

      File tempFile = File.createTempFile(FILENAME, SUFFIX);
      tempFile.deleteOnExit();

      BufferedWriter out = new BufferedWriter(new FileWriter(tempFile));
      out.write("Hello Cloud Files");
      out.close();

      SwiftObject object = swift.getApi().newSwiftObject();
      object.getInfo().setName(FILENAME + SUFFIX);
      object.setPayload(tempFile);

      swift.getApi().putObject(CONTAINER_PUBLISH, object);

      System.out.format("  %s%s%n", FILENAME, SUFFIX);
   }

   /**
    * This method will put your container on a Content Distribution Network and
    * make it 100% publicly accessible over the Internet.
    */
   private void enableCdnContainer() {
      System.out.format("Enable CDN Container%n");

      URI cdnURI = cloudFilesClient.enableCDN(CONTAINER_PUBLISH);

      System.out.format("  Go to %s/%s%s%n", cdnURI, FILENAME, SUFFIX);
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(blobStore.getContext(), true);
   }
}
