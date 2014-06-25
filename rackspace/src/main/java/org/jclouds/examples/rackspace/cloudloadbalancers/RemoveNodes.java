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
package org.jclouds.examples.rackspace.cloudloadbalancers;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.cloudloadbalancers.v1.CloudLoadBalancersApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.LoadBalancer;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.Node;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.LoadBalancerApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.NodeApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.predicates.LoadBalancerPredicates;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static org.jclouds.examples.rackspace.cloudloadbalancers.Constants.*;

/**
 * This example removes a Node from a Load Balancer.
 *
 */
public class RemoveNodes implements Closeable {
   private final CloudLoadBalancersApi clbApi;
   private final LoadBalancerApi lbApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      RemoveNodes removeNodes = new RemoveNodes(args[0], args[1]);

      try {
         LoadBalancer loadBalancer = removeNodes.getLoadBalancer();
         Set<Node> nodes = removeNodes.getNodes(loadBalancer);
         removeNodes.removeNodesFromLoadBalancer(nodes, loadBalancer);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         removeNodes.close();
      }
   }

   public RemoveNodes(String username, String apiKey) {
      clbApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CloudLoadBalancersApi.class);
      lbApi = clbApi.getLoadBalancerApiForZone(ZONE);
   }

   private LoadBalancer getLoadBalancer() {
      for (LoadBalancer loadBalancer: lbApi.list().concat()) {
         if (loadBalancer.getName().startsWith(NAME)) {
            return loadBalancer;
         }
      }

      throw new RuntimeException(NAME + " not found. Run a CreateLoadBalancer* example first.");
   }

   private Set<Node> getNodes(LoadBalancer loadBalancer) {
      NodeApi nodeApi = clbApi.getNodeApiForZoneAndLoadBalancer(ZONE, loadBalancer.getId());
      Set<Node> nodes = Sets.newHashSet();

      for (Node node: nodeApi.list().concat()) {
         if (node.getAddress().startsWith("10.180.1")) {
            nodes.add(node);
         }
      }

      return nodes;
   }

   private void removeNodesFromLoadBalancer(Set<Node> nodes, LoadBalancer loadBalancer) throws TimeoutException {
      System.out.format("Remove Nodes%n");

      NodeApi nodeApi = clbApi.getNodeApiForZoneAndLoadBalancer(ZONE, loadBalancer.getId());
      Iterable<Integer> nodeIds = Iterables.transform(nodes, new NodeToId());

      nodeApi.remove(nodeIds);

      // Wait for the Load Balancer to become Active before moving on
      // If you want to know what's happening during the polling, enable logging. See
      // /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      if (!LoadBalancerPredicates.awaitAvailable(lbApi).apply(loadBalancer)) {
         throw new TimeoutException("Timeout on loadBalancer: " + loadBalancer);
      }

      System.out.format("  %s%n", nodeIds);
   }

   private static class NodeToId implements Function<Node, Integer> {
      @Override
      public Integer apply(Node node) {
         return node.getId();
      }
   }

   /**
    * Always close your service when you're done with it.
    *
    * Note that closing quietly like this is not necessary in Java 7.
    * You would use try-with-resources in the main method instead.
    */
   public void close() throws IOException {
      Closeables.close(clbApi, true);
   }
}
