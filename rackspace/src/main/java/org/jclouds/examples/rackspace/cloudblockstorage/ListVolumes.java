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
package org.jclouds.examples.rackspace.cloudblockstorage;

import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.domain.Volume;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.PROVIDER;

/**
 * This example lists all volumes.
 */
public class ListVolumes implements Closeable {
   private final CinderApi cinderApi;
   private final Set<String> zones;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      ListVolumes listVolumes = new ListVolumes(args[0], args[1]);

      try {
         listVolumes.listVolumes();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         listVolumes.close();
      }
   }

   public ListVolumes(String username, String apiKey) {
      cinderApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CinderApi.class);
      zones = cinderApi.getConfiguredZones();
   }

   private void listVolumes() {
      System.out.format("List Volumes%n");

      for (String zone: zones) {
         System.out.format("  %s%n", zone);

         for (Volume volume: cinderApi.getVolumeApiForZone(zone).listInDetail()) {
            System.out.format("    %s%n", volume);
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
      Closeables.close(cinderApi, true);
   }
}
