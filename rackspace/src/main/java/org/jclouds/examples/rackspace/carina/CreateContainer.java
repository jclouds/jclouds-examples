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

import static org.jclouds.examples.rackspace.carina.Utils.getDockerApiFromCarinaDirectory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.jclouds.docker.DockerApi;
import org.jclouds.docker.domain.Config;
import org.jclouds.docker.domain.Container;
import org.jclouds.docker.domain.HostConfig;

import com.google.common.collect.ImmutableList;

/**
 * This example creates a Carina container
 *
 * To use, create/login at getcarina.com; then create a cluster and download the "get access" zip file.
 * Then extract the zip archive to a directory and pass the directory path as a parameter to main.
 */
public class CreateContainer {

   public static void main(String[] args) throws IOException {

      DockerApi dockerApi = getDockerApiFromCarinaDirectory(args[0]);

      /**
       * Specifying .publishAllPorts(true) in the HostConfig when *creating* the container is the simplest and
       * arguably best way to publish the ports this container will be using. Using .portBindings to specify particular
       * ports is somewhat more involved.
       *
       * However, because of https://github.com/docker/docker/issues/4635, TCP and UDP will be exposed on different
       * ports.
       */
      Container container = dockerApi.getContainerApi().createContainer("mumble",
            Config.builder()
                  .image("extra/mumble")
                  .hostConfig(
                        HostConfig.builder()
                              .publishAllPorts(true)
                              .build())
                  .env(
                        ImmutableList.of(
                              "MAX_USERS=50",
                              "SERVER_TEXT=Welcome to My Mumble Server",
                              "SUPW=" + UUID.randomUUID()
                        ))
            .build());

      String id = container.id();

      dockerApi.getContainerApi().startContainer(id);

      for(Entry<String, List<Map<String, String>>> portList : dockerApi.getContainerApi().inspectContainer(id).networkSettings().ports().entrySet()) {
         for(Map<String, String> port: portList.getValue()) {
            System.out.println("Port: " + portList.getKey() + " -> " + port.get("HostIp") + ":" + port.get("HostPort"));
         }
      }

      // Cleanup
      dockerApi.getContainerApi().stopContainer(id);
      dockerApi.getContainerApi().removeContainer(id);

      dockerApi.close();
   }
}
