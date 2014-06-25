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
import java.io.IOException;
import java.util.Map;

import org.jclouds.ContextBuilder;
import org.jclouds.openstack.swift.v1.domain.Container;
import org.jclouds.openstack.swift.v1.features.ContainerApi;
import org.jclouds.openstack.swift.v1.options.CreateContainerOptions;
import org.jclouds.rackspace.cloudfiles.v1.CloudFilesApi;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Closeables;

/**
 * Create a Cloud Files container with Cross Origin Resource Sharing (CORS) allowed. CORS container headers allow
 * users to upload files from one website--or origin--to your Cloud Files account. When you set the CORS headers on
 * your container, you tell Cloud Files which sites may post to your account, how often your container checks its
 * allowed sites list, and whether or not metadata headers can be passed with the objects.
 *
 */
public class CrossOriginResourceSharingContainer implements Closeable {
   private final CloudFilesApi cloudFiles;
   private final ContainerApi containerApi;

   /**
    * To get a username and API key see http://jclouds.apache.org/guides/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      CrossOriginResourceSharingContainer corsContainer = new CrossOriginResourceSharingContainer(args[0], args[1]);

      try {
         corsContainer.createCorsContainer();
         corsContainer.updateCorsContainer();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         corsContainer.close();
      }
   }

   public CrossOriginResourceSharingContainer(String username, String apiKey) {
      cloudFiles = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CloudFilesApi.class);
      containerApi = cloudFiles.getContainerApiForRegion(REGION);
   }

   /**
    * Create a Cross Origin Resource Sharing container.
    *
    * Access-Control-Allow-Origin:  Which URLs can make Cross Origin Requests. Format is http://www.example.com.
    *                               Separate URLs with a space. An asterisk (*) allows all.
    * Access-Control-Max-Age:       The maximum age for the browser to cache the options request, in seconds.
    * Access-Control-Allow-Headers: Which custom metadata headers you allow to be assigned to objects in this container.
    */
   private void createCorsContainer() {
      System.out.format("Create Cross Origin Resource Sharing Container%n");

      Multimap<String, String> headers = ImmutableMultimap.of(
            "Access-Control-Allow-Origin", "*",
            "Access-Control-Max-Age", "600",
            "Access-Control-Allow-Headers", "X-My-Header");
      CreateContainerOptions options = CreateContainerOptions.Builder.headers(headers);

      containerApi.create(CONTAINER, options);
      System.out.format("  %s%n", CONTAINER);

      Container container = containerApi.get(CONTAINER);
      System.out.format("    %s%n", container.getMetadata());
   }

   /**
    * Update a Cross Origin Resource Sharing container.
    */
   private void updateCorsContainer() {
      System.out.format("Update Cross Origin Resource Sharing Container%n");

      Map<String, String> corsMetadata = ImmutableMap.of(
            "Access-Control-Allow-Origin", "http://www.example.com",
            "Access-Control-Max-Age", "60",
            "Access-Control-Allow-Headers", "X-My-Other-Header");

      containerApi.updateMetadata(CONTAINER, corsMetadata);
      System.out.format("  %s%n", CONTAINER);

      Container container = containerApi.get(CONTAINER);
      System.out.format("    %s%n", container.getMetadata());
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(cloudFiles, true);
   }
}
