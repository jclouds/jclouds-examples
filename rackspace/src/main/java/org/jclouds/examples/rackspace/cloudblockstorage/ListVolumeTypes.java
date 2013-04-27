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
package org.jclouds.examples.rackspace.cloudblockstorage;

import static com.google.common.io.Closeables.closeQuietly;

import java.io.Closeable;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.CinderApiMetadata;
import org.jclouds.openstack.cinder.v1.CinderAsyncApi;
import org.jclouds.openstack.cinder.v1.domain.VolumeType;
import org.jclouds.rest.RestContext;

/**
 * This example lists all volume types.
 * 
 * @author Everett Toews
 */
public class ListVolumeTypes implements Closeable {
   private RestContext<CinderApi, CinderAsyncApi> cinder;
   private Set<String> zones;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username The second argument
    * (args[1]) must be your API key
    */
   public static void main(String[] args) {
      ListVolumeTypes listVolumeTypes = new ListVolumeTypes();

      try {
         listVolumeTypes.init(args);
         listVolumeTypes.listVolumeTypes();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         listVolumeTypes.close();
      }
   }

   private void init(String[] args) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the provider to "rackspace-cloudblockstorage-uk"
      String provider = "rackspace-cloudblockstorage-us";

      String username = args[0];
      String apiKey = args[1];

      cinder = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .build(CinderApiMetadata.CONTEXT_TOKEN);
      zones = cinder.getApi().getConfiguredZones();
   }

   private void listVolumeTypes() {
      System.out.println("List Volumes Types");

      for (String zone: zones) {
         System.out.println("  " + zone);

         for (VolumeType volumeType: cinder.getApi().getVolumeTypeApiForZone(zone).list()) {
            System.out.println("    " + volumeType);
         }
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(cinder);
   }
}
