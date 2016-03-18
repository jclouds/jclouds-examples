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
package org.jclouds.examples.rackspace.carina;

import static org.jclouds.compute.predicates.NodePredicates.runningInGroup;

import java.io.IOException;
import java.util.Set;

import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.docker.compute.options.DockerTemplateOptions;

/**
 * This example creates a container in Carina using the jclouds compute abstraction
 */

public class CreateComputeContainer {

   public static void main(String[] args) throws IOException, RunNodesException {
      // Get a context with docker that offers the portable ComputeService api
      ComputeServiceContext client = Utils.getComputeApiFromCarinaDirectory(args[0]);

      // Carina does not allow privileged mode containers
      DockerTemplateOptions dto = new DockerTemplateOptions();
      dto.privileged(false);

      // Use a known sshd image for demonstration purposes: sickp/apline-sshd
      Template template = client.getComputeService()
            .templateBuilder()
            .options(dto)
            .imageNameMatches("sickp/alpine-sshd")
            .build();

      // Run a couple nodes accessible via group container
      Set<? extends NodeMetadata> nodes = client.getComputeService().createNodesInGroup("jcloudscontainertest", 2, template);

      // Show the nodes
      for(NodeMetadata node : nodes) {
         System.out.println("Node: " + node.getName());
      }

      // Cleanup
      client.getComputeService().destroyNodesMatching(runningInGroup("jcloudscontainertest"));

      // Release resources
      client.close();
   }
}
