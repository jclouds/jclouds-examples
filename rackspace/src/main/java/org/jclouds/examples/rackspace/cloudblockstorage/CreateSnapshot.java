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
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.CinderApiMetadata;
import org.jclouds.openstack.cinder.v1.CinderAsyncApi;
import org.jclouds.openstack.cinder.v1.domain.Snapshot;
import org.jclouds.openstack.cinder.v1.domain.Volume;
import org.jclouds.openstack.cinder.v1.features.SnapshotApi;
import org.jclouds.openstack.cinder.v1.features.VolumeApi;
import org.jclouds.openstack.cinder.v1.options.CreateSnapshotOptions;
import org.jclouds.openstack.cinder.v1.predicates.SnapshotPredicates;
import org.jclouds.rest.RestContext;

/**
 * This example creates a snapshot of a volume.
 * 
 * @author Everett Toews
 */
public class CreateSnapshot implements Closeable {
   private RestContext<CinderApi, CinderAsyncApi> cinder;
   private VolumeApi volumeApi;
   private SnapshotApi snapshotApi;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username The second argument
    * (args[1]) must be your API key
    */
   public static void main(String[] args) {
      CreateSnapshot createSnapshot = new CreateSnapshot();

      try {
         createSnapshot.init(args);
         Volume volume = createSnapshot.getVolume();
         createSnapshot.createSnapshot(volume);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createSnapshot.close();
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
      volumeApi = cinder.getApi().getVolumeApiForZone(Constants.ZONE);
      snapshotApi = cinder.getApi().getSnapshotApiForZone(Constants.ZONE);
   }

   /**
    * @return Volume The Volume created in the CreateVolumeAndAttach example
    */
   private Volume getVolume() {
      for (Volume volume: volumeApi.list()) {
         if (volume.getName().startsWith(Constants.NAME)) {
            return volume;
         }
      }

      throw new RuntimeException(Constants.NAME + " not found. Run the CreateVolumeAndAttach example first.");
   }

   private void createSnapshot(Volume volume) throws TimeoutException {
      System.out.println("Create Snapshot");

      CreateSnapshotOptions options = CreateSnapshotOptions.Builder
            .name(Constants.NAME)
            .description("Snapshot of " + volume.getId());

      Snapshot snapshot = snapshotApi.create(volume.getId(), options);

      // Wait for the snapshot to become Available before moving on
      // If you want to know what's happening during the polling, enable logging. See
      // /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      if (!SnapshotPredicates.awaitAvailable(snapshotApi).apply(snapshot)) {
         throw new TimeoutException("Timeout on volume: " + volume);
      }

      System.out.println("  " + snapshot);
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(cinder);
   }
}
