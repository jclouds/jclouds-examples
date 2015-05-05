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
package org.jclouds.examples.rackspace.cloudnetworks;

import static org.jclouds.examples.rackspace.cloudnetworks.Constants.REGION;

import java.io.Closeable;
import java.io.IOException;

import org.jclouds.ContextBuilder;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.jclouds.openstack.neutron.v2.features.NetworkApi;

import com.google.common.io.Closeables;

/**
 * Demonstrates how to create a Poppy service on Rackspace (Rackspace CDN).
 * Cleans up the service on fail.
 */
public class CreateNetwork implements Closeable {
   private final NeutronApi neutronApi;

   /**
    * To get a username and API key see
    * http://jclouds.apache.org/guides/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      CreateNetwork createNetwork = new CreateNetwork(args[0], args[1]);

      try {
         createNetwork.createNetwork();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createNetwork.close();
      }
   }

   public CreateNetwork(String username, String apiKey) {
      neutronApi = ContextBuilder.newBuilder("rackspace-cloudnetworks-us")
            .credentials(username, apiKey)
            .buildApi(NeutronApi.class);
   }

   private void createNetwork() {
      NetworkApi networkApi = neutronApi.getNetworkApi(REGION);
      Network net = null;
      try {
         net = networkApi.create(Network.createBuilder("jclouds-test").build());
      } finally {
         // Cleanup
         if (net != null) {
            networkApi.delete(net.getId());
         }
      }
   }

   /**
    * Always close your service when you're done with it.
    *
    * Note that closing quietly like this is not necessary in Java 7.
    * You would use try-with-resources in the main method instead.
    */
   public void close() throws IOException {
      Closeables.close(neutronApi, true);
   }
}
