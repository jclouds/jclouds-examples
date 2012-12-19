/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
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

import static com.google.common.io.Closeables.closeQuietly;

import java.io.Closeable;
import java.util.Map;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.rest.RestContext;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;

/**
 * This example sets, gets, updates, and deletes metadata from a server.
 *  
 * @author Everett Toews
 */
public class ServerMetadata implements Closeable {
   private ComputeService compute;
   private RestContext<NovaApi, NovaAsyncApi> nova;
   private ServerApi serverApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      ServerMetadata serverMetadata = new ServerMetadata();

      try {
         serverMetadata.init(args);

         Server server = serverMetadata.getServer();
         serverMetadata.setMetadata(server);
         serverMetadata.updateMetadata(server);
         serverMetadata.deleteMetadata(server);
         serverMetadata.getMetadata(server);
      }
      finally {
         serverMetadata.close();
      }
   }

   private void init(String[] args) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the provider to "rackspace-cloudservers-uk"
      String provider = "rackspace-cloudservers-us";

      String username = args[0];
      String apiKey = args[1];

      ComputeServiceContext context = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .buildView(ComputeServiceContext.class);
      compute = context.getComputeService();
      nova = context.unwrap();
      serverApi = nova.getApi().getServerApiForZone(Constants.ZONE);
   }

   /**
    * @return The Server created in the CreateServer example
    */
   private Server getServer() {
      FluentIterable<? extends Server> servers = serverApi.listInDetail().concat();

      for (Server server: servers) {
         if (server.getName().startsWith(Constants.NAME)) {
            return server;
         }
      }

      throw new RuntimeException(Constants.NAME + " not found. Run the CreateServer example first.");
   }

   private void setMetadata(Server server) {
      System.out.println("Set Metadata");

      ImmutableMap<String, String> metadata = 
            ImmutableMap.<String, String> of("key1", "value1", "key2", "value2", "key3", "value3");
      Map<String, String> responseMetadata = serverApi.setMetadata(server.getId(), metadata);

      System.out.println("  " + responseMetadata);
   }

   private void updateMetadata(Server server) {
      System.out.println("Udpate Metadata");

      ImmutableMap<String, String> metadata = ImmutableMap.<String, String> of("key2", "new-value2");
      Map<String, String> responseMetadata = serverApi.updateMetadata(server.getId(), metadata);

      System.out.println("  " + responseMetadata);
   }

   private void deleteMetadata(Server server) {
      System.out.println("Delete Metadata");

      serverApi.deleteMetadata(server.getId(), "key3");
   }

   private void getMetadata(Server server) {
      System.out.println("Get Metadata");

      Map<String, String> metadata = serverApi.getMetadata(server.getId());

      System.out.println("  " + metadata);
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(compute.getContext());
   }
}
