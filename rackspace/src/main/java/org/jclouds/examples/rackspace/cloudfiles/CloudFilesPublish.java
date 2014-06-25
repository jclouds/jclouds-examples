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

import static org.jclouds.examples.rackspace.cloudfiles.Constants.CONTAINER_PUBLISH;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.FILENAME;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.REGION;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.SUFFIX;
import static org.jclouds.openstack.swift.v1.reference.SwiftHeaders.STATIC_WEB_ERROR;
import static org.jclouds.openstack.swift.v1.reference.SwiftHeaders.STATIC_WEB_INDEX;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.jclouds.ContextBuilder;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.jclouds.openstack.swift.v1.features.ObjectApi;
import org.jclouds.openstack.swift.v1.options.CreateContainerOptions;
import org.jclouds.openstack.swift.v1.reference.SwiftHeaders;
import org.jclouds.rackspace.cloudfiles.v1.CloudFilesApi;
import org.jclouds.rackspace.cloudfiles.v1.features.CDNApi;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteSource;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

/**
 * This example will create a container, put a file in it, and publish it on the internet!
 */
public class CloudFilesPublish implements Closeable {
   private final CloudFilesApi cloudFiles;

   /**
    * To get a username and API key see http://jclouds.apache.org/guides/rackspace/
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
      cloudFiles = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CloudFilesApi.class);
   }

   /**
    * This method will create a container in Cloud Files where you can store and
    * retrieve any kind of digital asset.
    */
   private void createContainer() {
      System.out.format("Create Container%n");

      Multimap<String, String> enableStaticWebHeaders =
            ImmutableMultimap.of(STATIC_WEB_INDEX, FILENAME + SUFFIX,
                                 STATIC_WEB_ERROR, "error.html");

      CreateContainerOptions opts = new CreateContainerOptions().headers(enableStaticWebHeaders);
      cloudFiles.getContainerApiForRegion(REGION).create(CONTAINER_PUBLISH, opts);

      System.out.format("  %s%n", CONTAINER_PUBLISH);
   }

   /**
    * This method will put a plain text object into the container.
    */
   private void createObjectFromFile() throws IOException {
      System.out.format("Create Object From File%n");

      File tempFile = File.createTempFile(FILENAME, SUFFIX);

      try {
         Files.write("Hello Cloud Files", tempFile, Charsets.UTF_8);

         ObjectApi objectApi = cloudFiles.getObjectApiForRegionAndContainer(REGION, CONTAINER_PUBLISH);

         ByteSource byteSource = Files.asByteSource(tempFile);
         Payload payload = Payloads.newByteSourcePayload(byteSource);

         objectApi.put(FILENAME + SUFFIX, payload);
      } finally {
         tempFile.delete();
      }
   }

   /**
    * This method will put your container on a Content Distribution Network and
    * make it 100% publicly accessible over the Internet.
    */
   private void enableCdnContainer() {
      System.out.format("Enable CDN Container%n");

      CDNApi cdnApi = cloudFiles.getCDNApiForRegion(REGION);
      URI cdnURI = cdnApi.enable(CONTAINER_PUBLISH);

      System.out.format("  Go to %s/%s%s%n", cdnURI, FILENAME, SUFFIX);
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(cloudFiles, true);
   }
}
