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
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.predicates.NodePredicates;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

/**
 * This example lists servers filtered by Predicates. Run the CreateServer example before this to get some results.
 *  
 * @author Everett Toews
 */
public class ListServersWithFiltering implements Closeable {
   private ComputeService compute;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      ListServersWithFiltering listServersWithFiltering = new ListServersWithFiltering();

      try {
         listServersWithFiltering.init(args);
         listServersWithFiltering.listServersByParentLocationId();
         listServersWithFiltering.listServersByNameStartsWith();
      }
      finally {
         listServersWithFiltering.close();
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
   }

   private void listServersByParentLocationId() {
      System.out.println("List Servers By Parent Location Id");

      Set<? extends NodeMetadata> servers = compute.listNodesDetailsMatching(NodePredicates
            .parentLocationId(Constants.ZONE));

      for (NodeMetadata nodeMetadata: servers) {
         System.out.println("  " + nodeMetadata);
      }
   }

   private void listServersByNameStartsWith() {
      System.out.println("List Servers By Name Starts With");

      Set<? extends NodeMetadata> servers = compute.listNodesDetailsMatching(nameStartsWith("jclouds-ex"));

      for (NodeMetadata nodeMetadata: servers) {
         System.out.println("  " + nodeMetadata);
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(compute.getContext());
   }

   public static Predicate<ComputeMetadata> nameStartsWith(final String prefix) {
      Preconditions.checkNotNull(prefix, "prefix must be defined");

      return new Predicate<ComputeMetadata>() {
         @Override
         public boolean apply(ComputeMetadata computeMetadata) {
            return computeMetadata.getName().startsWith(prefix);
         }

         @Override
         public String toString() {
            return "nameStartsWith(" + prefix + ")";
         }
      };
   }
}
