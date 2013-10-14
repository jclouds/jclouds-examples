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

import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.openstack.swift.CommonSwiftAsyncClient;
import org.jclouds.openstack.swift.CommonSwiftClient;
import org.jclouds.openstack.swift.domain.ContainerMetadata;
import org.jclouds.rest.RestContext;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

import static org.jclouds.examples.rackspace.cloudfiles.Constants.PROVIDER;

/**
 * List the Cloud Files containers associated with your account.
 *  
 * @author Everett Toews
 */
public class ListContainers implements Closeable {
   private final BlobStore blobStore;
   private final RestContext<CommonSwiftClient, CommonSwiftAsyncClient> swift;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      ListContainers listContainers = new ListContainers(args[0], args[1]);

      try {
         listContainers.listContainers();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         listContainers.close();
      }
   }

   public ListContainers(String username, String apiKey) {
      BlobStoreContext context = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildView(BlobStoreContext.class);
      blobStore = context.getBlobStore();
      swift = context.unwrap();
   }

   private void listContainers() {
      System.out.format("List Containers%n");

      Set<ContainerMetadata> containers = swift.getApi().listContainers();

      for (ContainerMetadata container: containers) {
         System.out.format("  %s%n", container);
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(blobStore.getContext(), true);
   }
}
