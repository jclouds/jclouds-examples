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

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.examples.rackspace.cloudservers.CloudServersPublish;
import org.jclouds.rackspace.cloudloadbalancers.v1.CloudLoadBalancersApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.AddNode;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.CreateLoadBalancer;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.LoadBalancer;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.Node;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.VirtualIP;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.VirtualIPWithId;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.LoadBalancerApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.predicates.LoadBalancerPredicates;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * This example creates a Load Balancer with new Cloud Servers on the Rackspace Cloud. 
 *  
 * @author Everett Toews
 */
public class CreateLoadBalancerWithNewServers {
   private CloudLoadBalancersApi clb;
   private LoadBalancerApi lbApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      CreateLoadBalancerWithNewServers createLoadBalancer = new CreateLoadBalancerWithNewServers();

      try {
         List<String> argsList = Lists.newArrayList(args);
         argsList.add("2"); // the number of Cloud Servers to start
         Set<? extends NodeMetadata> nodes = CloudServersPublish.getPublishedCloudServers(argsList);

         createLoadBalancer.init(args);
         Set<AddNode> addNodes = createLoadBalancer.createNodeRequests(nodes);
         createLoadBalancer.createLoadBalancer(addNodes);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createLoadBalancer.close();
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

   /**
    * AddNodes specify the nodes (Cloud Servers) that requests will be sent to by the Load Balancer.
    */
   private Set<AddNode> createNodeRequests(Set<? extends NodeMetadata> nodes) {
      Set<AddNode> addNodes = Sets.newHashSet();
      
      for (NodeMetadata node: nodes) {
         String privateAddress = node.getPrivateAddresses().iterator().next();
         
         AddNode addNode = AddNode.builder()
               .address(privateAddress)
               .condition(Node.Condition.ENABLED)
               .port(80)
               .weight(20)
               .build();
         
         addNodes.add(addNode);
      }
      
      return addNodes;
   }

   /**
    * Create a Load Balancer that randomly distributes requests to its nodes. Normally you will want to use a
    * different algorithm for your Load Balancers (see LoadBalancer.Algorithm) but random nicely demonstrates
    * how requests are sent to diffent nodes when you reload the "Go to" URL printed out in the terminal.   
    * 
    * To see an example of creating a Load Balancer with existing Cloud Servers see  
    * CreateLoadBalancerWithExistingServers.
    */
   private void createLoadBalancer(Set<AddNode> addNodes) throws TimeoutException {
      System.out.println("Create Cloud Load Balancer");

      CreateLoadBalancer createLB = CreateLoadBalancer.builder()
            .name(Constants.NAME)
            .protocol("HTTP")
            .port(80)
            .algorithm(LoadBalancer.Algorithm.RANDOM)
            .nodes(addNodes)
            .virtualIPType(VirtualIP.Type.PUBLIC)
            .build();
      
      LoadBalancer loadBalancer = lbApi.create(createLB);
      
      // Wait for the Load Balancer to become Active before moving on
      // If you want to know what's happening during the polling, enable logging. See
      // /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      if (!LoadBalancerPredicates.awaitAvailable(lbApi).apply(loadBalancer)) {
         throw new TimeoutException("Timeout on loadBalancer: " + loadBalancer);     
      }

      System.out.println("  " + loadBalancer);
      System.out.println("  Go to http://" + getVirtualIPv4(loadBalancer.getVirtualIPs()));
   }
   
   private String getVirtualIPv4(Set<VirtualIPWithId> set) {
      for (VirtualIPWithId virtualIP: set) {
         if (virtualIP.getType().equals(VirtualIP.Type.PUBLIC) && 
             virtualIP.getIpVersion().equals(VirtualIP.IPVersion.IPV4)) {
            return virtualIP.getAddress();
         }
      }
      
      throw new RuntimeException("Public IPv4 address not found.");
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(clb);
   }
}
