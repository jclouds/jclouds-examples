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

import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.cloudloadbalancers.v1.CloudLoadBalancersApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.LoadBalancer;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.LoadBalancerApi;

/**
 * This example lists all Load Balancers. 
 *  
 * @author Everett Toews
 */
public class ListLoadBalancers {
   private CloudLoadBalancersApi clb;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      ListLoadBalancers listLoadBalancers = new ListLoadBalancers();

      try {
         listLoadBalancers.init(args);
         listLoadBalancers.listLoadBalancers();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         listLoadBalancers.close();
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
   }

   private void listLoadBalancers() {
      System.out.println("List Load Balancers");
      
      for (String zone: clb.getConfiguredZones()) {
         System.out.println("  " + zone);
         
         LoadBalancerApi lbApi = clb.getLoadBalancerApiForZone(zone);
         
         for (LoadBalancer loadBalancer: lbApi.list().concat()) {
            System.out.println("    " + loadBalancer);
         }         
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(clb);
   }
}
