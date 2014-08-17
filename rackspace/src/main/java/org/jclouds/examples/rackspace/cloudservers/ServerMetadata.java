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
package org.jclouds.examples.rackspace.cloudservers;

import static org.jclouds.examples.rackspace.cloudservers.Constants.NAME;
import static org.jclouds.examples.rackspace.cloudservers.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudservers.Constants.ZONE;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Closeables;

/**
 * This example sets, gets, updates, and deletes metadata from a server.
 *
 */
public class ServerMetadata implements Closeable {
   private final ComputeService computeService;
   private final NovaApi novaApi;
   private final ServerApi serverApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      ServerMetadata serverMetadata = new ServerMetadata(args[0], args[1]);

      try {
         Server server = serverMetadata.getServer();
         serverMetadata.setMetadata(server);
         serverMetadata.updateMetadata(server);
         serverMetadata.deleteMetadata(server);
         serverMetadata.getMetadata(server);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         serverMetadata.close();
      }
   }

   public ServerMetadata(String username, String apiKey) {
      ComputeServiceContext context = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildView(ComputeServiceContext.class);
      computeService = context.getComputeService();
      novaApi = context.unwrapApi(NovaApi.class);
      serverApi = novaApi.getServerApiForZone(ZONE);
   }

   /**
    * @return The Server created in the CreateServer example
    */
   private Server getServer() {
      FluentIterable<? extends Server> servers = serverApi.listInDetail().concat();

      for (Server server: servers) {
         if (server.getName().startsWith(NAME)) {
            return server;
         }
      }

      throw new RuntimeException(NAME + " not found. Run the CreateServer example first.");
   }

   private void setMetadata(Server server) {
      System.out.format("Set Metadata%n");

      ImmutableMap<String, String> metadata = ImmutableMap.of(
            "key1", "value1",
            "key2", "value2",
            "key3", "value3");
      Map<String, String> responseMetadata = serverApi.setMetadata(server.getId(), metadata);

      System.out.format("  %s%n", responseMetadata);
   }

   private void updateMetadata(Server server) {
      System.out.format("Udpate Metadata%n");

      ImmutableMap<String, String> metadata = ImmutableMap.of("key2", "new-value2");
      Map<String, String> responseMetadata = serverApi.updateMetadata(server.getId(), metadata);

      System.out.format("  %s%n", responseMetadata);
   }

   private void deleteMetadata(Server server) {
      System.out.format("Delete Metadata%n");

      serverApi.deleteMetadata(server.getId(), "key3");
   }

   private void getMetadata(Server server) {
      System.out.format("Get Metadata%n");

      Map<String, String> metadata = serverApi.getMetadata(server.getId());

      System.out.format("  %s%n", metadata);
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(computeService.getContext(), true);
   }
}
