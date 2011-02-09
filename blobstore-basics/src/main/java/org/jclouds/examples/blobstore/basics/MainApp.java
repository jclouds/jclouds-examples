/**
 *
 * Copyright (C) 2010 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.jclouds.examples.blobstore.basics;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.jclouds.atmos.AtmosAsyncClient;
import org.jclouds.atmos.AtmosClient;
import org.jclouds.azureblob.AzureBlobAsyncClient;
import org.jclouds.azureblob.AzureBlobClient;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.domain.StorageType;
import org.jclouds.blobstore.util.BlobStoreUtils;
import org.jclouds.openstack.swift.SwiftAsyncClient;
import org.jclouds.openstack.swift.SwiftClient;
import org.jclouds.rest.RestContext;
import org.jclouds.s3.S3AsyncClient;
import org.jclouds.s3.S3Client;

import com.google.common.collect.Iterables;

/**
 * Demonstrates the use of {@link BlobStore}.
 * 
 * Usage is: java MainApp \"provider\" \"identity\" \"credential\" \"containerName\"
 * 
 * @author Carlos Fernandes
 * @author Adrian Cole
 */
public class MainApp {

   public static int PARAMETERS = 4;
   public static String INVALID_SYNTAX = "Invalid number of parameters. Syntax is: \"provider\" \"identity\" \"credential\" \"containerName\" ";

   public static void main(String[] args) throws IOException {

      if (args.length < PARAMETERS)
         throw new IllegalArgumentException(INVALID_SYNTAX);

      // Args

      String provider = args[0];
      if (!Iterables.contains(BlobStoreUtils.getSupportedProviders(), provider))
         throw new IllegalArgumentException("provider " + provider + " not in supported list: "
                  + BlobStoreUtils.getSupportedProviders());
      String identity = args[1];
      String credential = args[2];
      String containerName = args[3];

      // Init
      BlobStoreContext context = new BlobStoreContextFactory().createContext(provider, identity, credential);

      try {

         // Create Container
         BlobStore blobStore = context.getBlobStore();
         blobStore.createContainerInLocation(null, containerName);

         // Add Blob
         Blob blob = blobStore.newBlob("test");
         blob.setPayload("testdata");
         blobStore.putBlob(containerName, blob);

         // List Container
         for (StorageMetadata resourceMd : blobStore.list()) {
            if (resourceMd.getType() == StorageType.CONTAINER || resourceMd.getType() == StorageType.FOLDER) {
               // Use Map API
               Map<String, InputStream> containerMap = context.createInputStreamMap(resourceMd.getName());
               System.out.printf("  %s: %s entries%n", resourceMd.getName(), containerMap.size());
            }
         }

         // Use Provider API
         if (context.getProviderSpecificContext().getApi() instanceof S3Client) {
            RestContext<S3Client, S3AsyncClient> providerContext = context.getProviderSpecificContext();
            providerContext.getApi().getBucketLogging(containerName);
         } else if (context.getProviderSpecificContext().getApi() instanceof SwiftClient) {
            RestContext<SwiftClient, SwiftAsyncClient> providerContext = context.getProviderSpecificContext();
            providerContext.getApi().getObjectInfo(containerName, "test");
         } else if (context.getProviderSpecificContext().getApi() instanceof AzureBlobClient) {
            RestContext<AzureBlobClient, AzureBlobAsyncClient> providerContext = context.getProviderSpecificContext();
            providerContext.getApi().getBlobProperties(containerName, "test");
         } else if (context.getProviderSpecificContext().getApi() instanceof AtmosClient) {
            RestContext<AtmosClient, AtmosAsyncClient> providerContext = context.getProviderSpecificContext();
            providerContext.getApi().getSystemMetadata(containerName + "/test");
         }
      } finally {
         // Close connecton
         context.close();
         System.exit(0);
      }

   }
}
