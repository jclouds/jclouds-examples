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

import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import static org.jclouds.compute.config.ComputeServiceProperties.POLL_INITIAL_PERIOD;
import static org.jclouds.compute.config.ComputeServiceProperties.POLL_MAX_PERIOD;
import static org.jclouds.compute.predicates.NodePredicates.inGroup;
import static org.jclouds.examples.rackspace.cloudservers.Constants.*;

/**
 * This example destroys the server created in the CreateServer example.
 *
 */
public class DeleteServer implements Closeable {
   private ComputeService computeService;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      DeleteServer deleteServer = new DeleteServer(args[0], args[1]);

      try {
         deleteServer.deleteServer();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         deleteServer.close();
      }
   }

   public DeleteServer(String username, String apiKey) {
      // These properties control how often jclouds polls for a status udpate
      Properties overrides = new Properties();
      overrides.setProperty(POLL_INITIAL_PERIOD, POLL_PERIOD_TWENTY_SECONDS);
      overrides.setProperty(POLL_MAX_PERIOD, POLL_PERIOD_TWENTY_SECONDS);

      ComputeServiceContext context = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .overrides(overrides)
            .buildView(ComputeServiceContext.class);
      computeService = context.getComputeService();
   }

   /**
    * This will delete all servers in group {@link Constants#NAME}
    */
   private void deleteServer() {
      System.out.format("Delete Server%n");

      // This method will continue to poll for the server status and won't return until this server is DELETED
      // If you want to know what's happening during the polling, enable logging. See
      // /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      Set<? extends NodeMetadata> servers = computeService.destroyNodesMatching(inGroup(NAME));

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
}
