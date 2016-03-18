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

import org.jclouds.docker.DockerApi;
import org.jclouds.docker.domain.ContainerSummary;

/**
 * This example lists Carina containers
 *
 * To use, create/login at getcarina.com; then create a cluster and download the "get access" zip file.
 * Then extract the zip archive to a directory and pass the directory path as a parameter to main.
 */
public class ListContainer {

   public static void main(String[] args) throws IOException {

      DockerApi dockerApi = getDockerApiFromCarinaDirectory(args[0]);

      for( ContainerSummary c : dockerApi.getContainerApi().listContainers()) {
         System.out.println(c);
      }

      dockerApi.close();
   }




}
