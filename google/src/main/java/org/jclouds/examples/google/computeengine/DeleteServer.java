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
package org.jclouds.examples.google.computeengine;

import com.google.common.io.Closeables;
import com.google.common.io.Files;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.Set;

import static org.jclouds.compute.config.ComputeServiceProperties.POLL_INITIAL_PERIOD;
import static org.jclouds.compute.config.ComputeServiceProperties.POLL_MAX_PERIOD;
import static org.jclouds.compute.predicates.NodePredicates.inGroup;
import static org.jclouds.examples.google.computeengine.Constants.*;

/**
 * This example destroys the server created in the CreateServer example.
 */
public class DeleteServer implements Closeable {
   private ComputeService computeService;

   /**
    * The first argument (args[0]) must be your service account email address
    * The second argument (args[1]) must a path to your service account
    *     private key PEM file (without a password).
    */
   public static void main(String[] args) throws IOException {
	  String key = Files.toString(new File(args[1]), Charset.defaultCharset());

	  DeleteServer deleteServer = new DeleteServer(args[0], key);

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

   public DeleteServer(String serviceAccountEmailAddress, String serviceAccountKey) {
      // These properties control how often jclouds polls for a status udpate
      Properties overrides = new Properties();
      overrides.setProperty(POLL_INITIAL_PERIOD, POLL_PERIOD_TWENTY_SECONDS);
      overrides.setProperty(POLL_MAX_PERIOD, POLL_PERIOD_TWENTY_SECONDS);

      ComputeServiceContext context = ContextBuilder.newBuilder(PROVIDER)
            .credentials(serviceAccountEmailAddress, serviceAccountKey)
            .overrides(overrides)
            .buildView(ComputeServiceContext.class);
      computeService = context.getComputeService();
   }

   /**
    * This will delete all servers in group {@link Constants#NAME}
    */
   private void deleteServer() {
      System.out.format("Delete Servers in group %s%n", NAME);

      // This method will continue to poll for the server status and won't return until this server
      // is DELETED.
      Set<? extends NodeMetadata> servers = computeService.destroyNodesMatching(inGroup(NAME));

      for (NodeMetadata server : servers) {
         System.out.format("  %s%n", server);
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(computeService.getContext(), true);
   }
}
