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

import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.poppy.v1.PoppyApi;
import org.jclouds.openstack.poppy.v1.domain.Service;
import org.jclouds.openstack.poppy.v1.features.ServiceApi;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;

/**
 * Updates a Poppy service. (Rackspace CDN).
 * This operation internally diffs a target Service with a currently exsiting source service
 * and automatically sends the json-patch diff to Poppy, which applies it.
 */
public class UpdateService implements Closeable {
   private final PoppyApi cdnApi;
   private final ServiceApi serviceApi;

   /**
    * To get a username and API key see
    * http://jclouds.apache.org/guides/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      UpdateService createPolicy = new UpdateService(args[0], args[1]);

      try {
         createPolicy.updateService();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createPolicy.close();
      }
   }

   public UpdateService(String username, String apiKey) {
      cdnApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(PoppyApi.class);

      serviceApi = cdnApi.getServiceApi();
   }

   private void updateService() {
      try {
         Service serviceList = serviceApi.list().concat().toSet().iterator().next();

         System.out.println("Update service " + serviceList.getId());
         System.out.println("Status: " + serviceList.getStatus());

         org.jclouds.openstack.poppy.v1.domain.UpdateService updated = serviceList.toUpdatableService().name("updated_name").build();
         serviceApi.update(serviceList.getId(), serviceList, updated);
      } finally {
         // Do nothing on fail.
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
