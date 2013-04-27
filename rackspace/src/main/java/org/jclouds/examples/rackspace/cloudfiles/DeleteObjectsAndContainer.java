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
package org.jclouds.examples.rackspace.cloudfiles;

import static com.google.common.io.Closeables.closeQuietly;

import java.io.Closeable;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.openstack.swift.CommonSwiftAsyncClient;
import org.jclouds.openstack.swift.CommonSwiftClient;
import org.jclouds.openstack.swift.domain.ContainerMetadata;
import org.jclouds.openstack.swift.domain.ObjectInfo;
import org.jclouds.openstack.swift.options.ListContainerOptions;
import org.jclouds.rest.RestContext;

/**
 * Delete the objects from the CreateObjects example and delete the object storage container from the 
 * CreateContainer example.
 *  
 * @author Everett Toews
 */
public class DeleteObjectsAndContainer implements Closeable {
   private BlobStore storage;
   private RestContext<CommonSwiftClient, CommonSwiftAsyncClient> swift;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      DeleteObjectsAndContainer deleteObjectsAndContainer = new DeleteObjectsAndContainer();

      try {
         deleteObjectsAndContainer.init(args);
         deleteObjectsAndContainer.deleteObjectsAndContainer();
      }
      finally {
         deleteObjectsAndContainer.close();
      }
   }

   private void init(String[] args) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the provider to "cloudfiles-uk"
      String provider = "cloudfiles-us";

      String username = args[0];
      String apiKey = args[1];

      BlobStoreContext context = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .buildView(BlobStoreContext.class);
      storage = context.getBlobStore();
      swift = context.unwrap();
   }

   /**
    * This will delete all containers that start with {@link CONTAINER} and the objects within those containers.
    */
   private void deleteObjectsAndContainer() {
      System.out.println("Delete Container");

      Set<ContainerMetadata> containers = swift.getApi()
            .listContainers(ListContainerOptions.Builder.withPrefix(Constants.CONTAINER));

      for (ContainerMetadata container: containers) {
         System.out.println("  " + container.getName());

         Set<ObjectInfo> objects = swift.getApi().listObjects(container.getName());

         for (ObjectInfo object: objects) {
            System.out.println("    " + object.getName());

            swift.getApi().removeObject(container.getName(), object.getName());
         }

         swift.getApi().deleteContainerIfEmpty(container.getName());
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(storage.getContext());
   }
}
