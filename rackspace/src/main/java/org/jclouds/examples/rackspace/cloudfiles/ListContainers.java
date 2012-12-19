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
import org.jclouds.rest.RestContext;

/**
 * List the object storage containers associated with your account.
 *  
 * @author Everett Toews
 */
public class ListContainers implements Closeable {
   private BlobStore storage;
   private RestContext<CommonSwiftClient, CommonSwiftAsyncClient> swift;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      ListContainers listContainers = new ListContainers();

      try {
         listContainers.init(args);
         listContainers.listContainers();
      }
      finally {
         listContainers.close();
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

   private void listContainers() {
      System.out.println("List Containers");
      Set<ContainerMetadata> containers = swift.getApi().listContainers();

      for (ContainerMetadata container: containers) {
         System.out.println("  " + container);
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(storage.getContext());
   }
}
