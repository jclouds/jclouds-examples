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

import static org.jclouds.examples.rackspace.cloudnetworks.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudnetworks.Constants.REGION;

import java.io.Closeable;
import java.io.IOException;

import org.jclouds.ContextBuilder;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.IP;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.jclouds.openstack.neutron.v2.domain.Port;
import org.jclouds.openstack.neutron.v2.domain.Subnet;
import org.jclouds.openstack.neutron.v2.features.NetworkApi;
import org.jclouds.openstack.neutron.v2.features.PortApi;
import org.jclouds.openstack.neutron.v2.features.SubnetApi;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;

/**
 * Demonstrates how to create a Poppy service on Rackspace (Rackspace CDN).
 * Cleans up the service on fail.
 */
public class CreatePort implements Closeable {
   private final NeutronApi neutronApi;

   /**
    * To get a username and API key see
    * http://jclouds.apache.org/guides/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      CreatePort createPort = new CreatePort(args[0], args[1]);

      try {
         createPort.createPort();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createPort.close();
      }
   }

   public CreatePort(String username, String apiKey) {
      neutronApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(NeutronApi.class);
   }

   private void createPort() {
      NetworkApi networkApi = neutronApi.getNetworkApi(REGION);
      SubnetApi subnetApi = neutronApi.getSubnetApi(REGION);
      PortApi portApi = neutronApi.getPortApi(REGION);

      Network net = null;
      Subnet subnet = null;
      Port port = null;

      try {
         // Create a network first. The subnet will be created on the network.
         net = networkApi.create(Network.createBuilder("jclouds-test").build());

         subnet = subnetApi.create(
               Subnet.createBuilder(net.getId(), "192.168.0.0/30").ipVersion(4)
                     .name("JClouds-Live-IPv4-Subnet").build()
         );

         port = portApi.create(
               Port.createBuilder(net.getId()).name("JClouds-Live-IPv4-Port")
                     .fixedIps( ImmutableSet.of(IP.builder().subnetId(subnet.getId()).build() )).build()
         );

      } finally {
         // Cleanup
         if (port != null) {
            portApi.delete(port.getId());
         }
         if (subnet != null) {
            subnetApi.delete(subnet.getId());
         }
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
