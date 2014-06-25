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

import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.cloudloadbalancers.v1.CloudLoadBalancersApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.LoadBalancer;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.LoadBalancerApi;

import java.io.Closeable;
import java.io.IOException;

import static org.jclouds.examples.rackspace.cloudloadbalancers.Constants.PROVIDER;

/**
 * This example lists all Load Balancers.
 *
 */
public class ListLoadBalancers implements Closeable {
   private final CloudLoadBalancersApi clbApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      ListLoadBalancers listLoadBalancers = new ListLoadBalancers(args[0], args[1]);

      try {
         listLoadBalancers.listLoadBalancers();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         listLoadBalancers.close();
      }
   }

   public ListLoadBalancers(String username, String apiKey) {
      clbApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CloudLoadBalancersApi.class);
   }

   private void listLoadBalancers() {
      System.out.format("List Load Balancers%n");

      for (String zone: clbApi.getConfiguredZones()) {
         System.out.format("  %s%n", zone);

         LoadBalancerApi lbApi = clbApi.getLoadBalancerApiForZone(zone);

         for (LoadBalancer loadBalancer: lbApi.list().concat()) {
            System.out.format("    %s%n", loadBalancer);
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
      Closeables.close(clbApi, true);
   }
}
