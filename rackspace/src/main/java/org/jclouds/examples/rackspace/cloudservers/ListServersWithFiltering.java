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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.predicates.NodePredicates;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

import static org.jclouds.examples.rackspace.cloudservers.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudservers.Constants.ZONE;

/**
 * This example lists servers filtered by Predicates. Run the CreateServer example before this to get some results.
 *
 */
public class ListServersWithFiltering implements Closeable {
   private final ComputeService computeService;


   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      ListServersWithFiltering listServersWithFiltering = new ListServersWithFiltering(args[0], args[1]);

      try {
         listServersWithFiltering.listServersByParentLocationId();
         listServersWithFiltering.listServersByNameStartsWith();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         listServersWithFiltering.close();
      }
   }

   public ListServersWithFiltering(String username, String apiKey) {
      ComputeServiceContext context = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildView(ComputeServiceContext.class);
      computeService = context.getComputeService();

   }

   private void listServersByParentLocationId() {
      System.out.format("List Servers By Parent Location Id%n");

      Set<? extends NodeMetadata> servers = computeService.listNodesDetailsMatching(NodePredicates
            .parentLocationId(ZONE));

      for (NodeMetadata nodeMetadata: servers) {
         System.out.format("  %s%n", nodeMetadata);
      }
   }

   private void listServersByNameStartsWith() {
      System.out.format("List Servers By Name Starts With%n");

      Set<? extends NodeMetadata> servers = computeService.listNodesDetailsMatching(nameStartsWith("jclouds-ex"));

      for (NodeMetadata nodeMetadata: servers) {
         System.out.format("  %s%n", nodeMetadata);
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(computeService.getContext(), true);
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
