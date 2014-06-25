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
import org.jclouds.openstack.cinder.v1.domain.Snapshot;
import org.jclouds.openstack.cinder.v1.features.SnapshotApi;
import org.jclouds.openstack.cinder.v1.predicates.SnapshotPredicates;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.*;

/**
 * This example deletes a snapshot.
 */
public class DeleteSnapshot implements Closeable {
   private final CinderApi cinderApi;
   private final SnapshotApi snapshotApi;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      DeleteSnapshot deleteSnapshot = new DeleteSnapshot(args[0], args[1]);

      try {
         Snapshot snapshot = deleteSnapshot.getSnapshot();
         deleteSnapshot.deleteSnapshot(snapshot);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         deleteSnapshot.close();
      }
   }

   public DeleteSnapshot(String username, String apiKey) {
      cinderApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CinderApi.class);
      snapshotApi = cinderApi.getSnapshotApiForZone(ZONE);
   }

   /**
    * @return Snapshot The Snapshot created in the CreateSnapshot example
    */
   private Snapshot getSnapshot() {
      for (Snapshot snapshot : snapshotApi.list()) {
         if (snapshot.getName().startsWith(NAME)) {
            return snapshot;
         }
      }

      throw new RuntimeException(NAME + " not found. Run the CreateSnapshot example first.");
   }

   private void deleteSnapshot(Snapshot snapshot) throws TimeoutException {
      System.out.format("Delete Snapshot%n");

      boolean result = snapshotApi.delete(snapshot.getId());

      // Wait for the snapshot to be deleted before moving on
      // If you want to know what's happening during the polling, enable logging.
      // See /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      if (!SnapshotPredicates.awaitDeleted(snapshotApi).apply(snapshot)) {
         throw new TimeoutException("Timeout on snapshot: " + snapshot);
      }

      System.out.format("  %s%n", result);
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
