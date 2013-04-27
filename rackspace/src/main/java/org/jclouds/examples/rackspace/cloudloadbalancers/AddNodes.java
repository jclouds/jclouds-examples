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
package org.jclouds.examples.rackspace.cloudloadbalancers;

import static com.google.common.io.Closeables.closeQuietly;

import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.cloudloadbalancers.v1.CloudLoadBalancersApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.AddNode;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.LoadBalancer;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.Node;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.LoadBalancerApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.NodeApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.predicates.LoadBalancerPredicates;

import com.google.common.collect.Sets;

/**
 * This example adds a Node to a Load Balancer. 
 *  
 * @author Everett Toews
 */
public class AddNodes {
   private CloudLoadBalancersApi clb;
   private LoadBalancerApi lbApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      AddNodes addNodes = new AddNodes();

      try {
         addNodes.init(args);
         LoadBalancer loadBalancer = addNodes.getLoadBalancer();
         Set<AddNode> addNodeSet = addNodes.createAddNodes();
         addNodes.addNodesToLoadBalancer(addNodeSet, loadBalancer);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         addNodes.close();
      }
   }

   private void init(String[] args) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the provider to "rackspace-cloudloadbalancers-uk"
      String provider = "rackspace-cloudloadbalancers-us";

      String username = args[0];
      String apiKey = args[1];

      clb = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .buildApi(CloudLoadBalancersApi.class);
      lbApi = clb.getLoadBalancerApiForZone(Constants.ZONE);
   }

   private LoadBalancer getLoadBalancer() {
      for (LoadBalancer loadBalancer: lbApi.list().concat()) {
         if (loadBalancer.getName().startsWith(Constants.NAME)) {
            return loadBalancer;
         }
      }
      
      throw new RuntimeException(Constants.NAME + " not found. Run a CreateLoadBalancer* example first.");
   }

   /**
    * AddNodes are the nodes (Cloud Servers) that receive requests sent from the Load Balancer.
    * 
    * The IPv4 addresses in the AddNodes below are only *examples* of addresses that you would use when creating
    * a Load Balancer. You would do this if you had existing Cloud Servers and stored their IPv4
    * addresses as configuration data.
    */
   private Set<AddNode> createAddNodes() {
      AddNode addNode11 = AddNode.builder()
            .address("10.180.1.1")
            .condition(Node.Condition.DISABLED)
            .port(80)
            .weight(20)
            .build();
      
      AddNode addNode12 = AddNode.builder()
            .address("10.180.1.2")
            .condition(Node.Condition.ENABLED)
            .port(80)
            .weight(20)
            .build();

      return Sets.newHashSet(addNode11, addNode12);      
   }

   /**
    * If you try to visit the IPv4 address of the Load Balancer itself, you will see a "Service Unavailable" message 
    * because the nodes from the createNodeRequests() don't really exist.
    * 
    * To see an example of creating Cloud Servers and a Load Balancer at the same time see 
    * CreateLoadBalancerWithNewServers.  
    */
   private void addNodesToLoadBalancer(Set<AddNode> addNodes, LoadBalancer loadBalancer) throws TimeoutException {
      System.out.println("Add Nodes");

      NodeApi nodeApi = clb.getNodeApiForZoneAndLoadBalancer(Constants.ZONE, loadBalancer.getId());
      Set<Node> nodes = nodeApi.add(addNodes);
      
      // Wait for the Load Balancer to become Active before moving on
      // If you want to know what's happening during the polling, enable logging. See
      // /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      if (!LoadBalancerPredicates.awaitAvailable(lbApi).apply(loadBalancer)) {
         throw new TimeoutException("Timeout on loadBalancer: " + loadBalancer);     
      }
      
      for (Node node: nodes) {         
         System.out.println("  " + node);
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(clb);
   }
}
