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
package org.jclouds.examples.rackspace.clouddatabases;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.openstack.trove.v1.TroveApi;
import org.jclouds.openstack.trove.v1.domain.Instance;
import org.jclouds.openstack.trove.v1.features.InstanceApi;
import org.jclouds.openstack.trove.v1.features.UserApi;

import com.google.common.io.Closeables;

import static org.jclouds.examples.rackspace.clouddatabases.Constants.*;

/**
 * This example will delete the User created in the CreateUser example.
 */
public class DeleteUser implements Closeable {
   private final TroveApi troveApi;
   private final InstanceApi instanceApi;
   private final UserApi userApi;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument  (args[0]) must be your username.
    * The second argument (args[1]) must be your API key.
    */
   public static void main(String[] args) throws IOException {
      DeleteUser deleteUser = new DeleteUser(args[0], args[1]);

      try {
         deleteUser.deleteUser();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         deleteUser.close();
      }
   }

   public DeleteUser(String username, String apiKey) {
      troveApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(TroveApi.class);

      instanceApi = troveApi.getInstanceApiForZone(ZONE);
      userApi = troveApi.getUserApiForZoneAndInstance(ZONE, getInstance().getId());
   }

   /**
    * @return Instance The Instance created in the CreateInstance example.
    */
   private Instance getInstance() {
      for (Instance instance : instanceApi.list()) {
         if (instance.getName().startsWith(NAME)) {
            return instance;
         }
      }

      throw new RuntimeException(NAME + " not found. Run the CreateInstance example first.");
   }

   private void deleteUser() throws TimeoutException {
      System.out.format("Delete User%n");

      boolean result = userApi.delete(NAME);

      System.out.format("  %s%n", result);
   }

   /**
    * Always close your service when you're done with it.
    *
    * Note that closing quietly like this is not necessary in Java 7.
    * You would use try-with-resources in the main method instead.
    */
   public void close() throws IOException {
      Closeables.close(troveApi, true);
   }
}
