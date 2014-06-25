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
package org.jclouds.examples.rackspace.cloudfiles;

import static org.jclouds.examples.rackspace.cloudfiles.Constants.CONTAINER;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.REGION;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import org.jclouds.ContextBuilder;
import org.jclouds.openstack.swift.v1.domain.Container;
import org.jclouds.openstack.swift.v1.domain.ObjectList;
import org.jclouds.openstack.swift.v1.domain.SwiftObject;
import org.jclouds.openstack.swift.v1.features.ObjectApi;
import org.jclouds.openstack.swift.v1.options.ListContainerOptions;
import org.jclouds.rackspace.cloudfiles.v1.CloudFilesApi;

import com.google.common.io.Closeables;

/**
 * Delete the objects from the CreateObjects example and delete the Cloud Files container from the
 * CreateContainer example.
 *
 */
public class DeleteObjectsAndContainer implements Closeable {
   private final CloudFilesApi cloudFiles;

   /**
    * To get a username and API key see http://jclouds.apache.org/guides/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      DeleteObjectsAndContainer deleteObjectsAndContainer = new DeleteObjectsAndContainer(args[0], args[1]);

      try {
         deleteObjectsAndContainer.deleteObjectsAndContainer();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         deleteObjectsAndContainer.close();
      }
   }

   public DeleteObjectsAndContainer(String username, String apiKey) {
      cloudFiles = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CloudFilesApi.class);
   }

   /**
    * This will delete all containers that start with {@link Constants#CONTAINER} and the objects within those containers.
    */
   private void deleteObjectsAndContainer() {
      System.out.format("Delete Container%n");

      List<Container> containers = cloudFiles.getContainerApiForRegion(REGION)
            .list(ListContainerOptions.Builder.prefix(CONTAINER)).toList();

      for (Container container: containers) {
         System.out.format("  %s%n", container.getName());

         ObjectApi objectApi = cloudFiles.getObjectApiForRegionAndContainer(REGION, container.getName());
         ObjectList objects = objectApi.list(ListContainerOptions.NONE);

         for (SwiftObject object: objects) {
            System.out.format("    %s%n", object.getName());
            objectApi.delete(object.getName());
         }

         cloudFiles.getContainerApiForRegion(REGION).deleteIfEmpty(container.getName());
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(cloudFiles, true);
   }
}
