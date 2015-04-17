/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.examples.rackspace.cdn;

import static org.jclouds.examples.rackspace.cdn.Constants.PROVIDER;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.poppy.v1.PoppyApi;
import org.jclouds.openstack.poppy.v1.domain.Caching;
import org.jclouds.openstack.poppy.v1.domain.Domain;
import org.jclouds.openstack.poppy.v1.domain.Origin;
import org.jclouds.openstack.poppy.v1.domain.Restriction;
import org.jclouds.openstack.poppy.v1.features.FlavorApi;
import org.jclouds.openstack.poppy.v1.features.ServiceApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Module;

/**
 * Demonstrates how to create a Poppy service on Rackspace (Rackspace CDN).
 * Cleans up the service on fail.
 */
public class CreateService implements Closeable {
   private final PoppyApi cdnApi;
   private final FlavorApi flavorApi;
   private final ServiceApi serviceApi;

   /**
    * To get a username and API key see
    * http://jclouds.apache.org/guides/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      CreateService createPolicy = new CreateService(args[0], args[1]);

      try {
         createPolicy.createService();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createPolicy.close();
      }
   }

   public CreateService(String username, String apiKey) {
      cdnApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(PoppyApi.class);

      serviceApi = cdnApi.getServiceApi();
      flavorApi = cdnApi.getFlavorApi();
   }

   private void createService() {

      String serviceId = null;
      try {
         URI serviceURI = serviceApi.create(
               org.jclouds.openstack.poppy.v1.domain.CreateService.builder()
                     .name("jclouds_test_service")
                     .domains(
                           ImmutableList.of(
                                 // Domain might be taken already - will result in failure. Use UUID in the test
                                 // to get a random domain.
                                 Domain.builder().domain("www.example" + UUID.randomUUID() + ".com").build()))
                     .origins(ImmutableList.of(
                           Origin.builder()
                                 .origin("example" + UUID.randomUUID() + ".com")
                                 .build()))
                     .caching(ImmutableList.<Caching>of())
                     .restrictions(ImmutableList.<Restriction>of())
                     .flavorId(flavorApi.list().first().get().getId())
                     .build()
         );
         String path = serviceURI.getPath();
         serviceId = path.substring(path.lastIndexOf('/') + 1);

         System.out.println("Created Poppy service with id " + serviceId);
         System.out.println("Waiting for service to deploy");

         // Not available in 1.9.0; available in SNAPSHOT
         //ServicePredicates.awaitDeployed(serviceApi).apply(serviceApi.get(serviceId));
         Uninterruptibles.sleepUninterruptibly(30, TimeUnit.SECONDS);
      } catch (Exception e) {
         // Cleanup on fail
         if (serviceId != null) {
            serviceApi.delete(serviceId);
         }
      }
   }

   /**
    * Always close your service when you're done with it.
    *
    * Note that closing quietly like this is not necessary in Java 7.
    * You would use try-with-resources in the main method instead.
    */
   public void close() throws IOException {
      Closeables.close(cdnApi, true);
   }
}
