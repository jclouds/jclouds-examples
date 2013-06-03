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

import com.google.common.io.Closeables;

/**
 * This example grants root permissions to the instance created in the CreateInstance example.
 * 
 * @author Zack Shoylev
 */
public class GrantRootAccess implements Closeable {
   private TroveApi api;
   private InstanceApi instanceApi;

   /**
    * To get a username and API key see 
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument  (args[0]) must be your username.
    * The second argument (args[1]) must be your API key.
    * @throws IOException 
    */
   public static void main(String[] args) throws IOException {
      
      GrantRootAccess grantRootAccess = new GrantRootAccess();

      try {
         grantRootAccess.init(args);
         Instance instance = grantRootAccess.getInstance();
         grantRootAccess.grantRootAccess(instance);
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         grantRootAccess.close();
      }
   }

   private void init(String[] args) {
      // The provider configures jclouds to use the Rackspace Cloud (US).
      // To use the Rackspace Cloud (UK) set the provider to "rackspace-clouddatabases-uk".
      String provider = "rackspace-clouddatabases-us";

      String username = args[0];
      String apiKey = args[1];
      
      api = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .buildApi(TroveApi.class);
      
      instanceApi = api.getInstanceApiForZone(Constants.ZONE);
   }

   /**
    * @return Instance The Instance created in the CreateInstance example.
    */
   private Instance getInstance() {
      for (Instance instance: instanceApi.list()) {
         if (instance.getName().startsWith(Constants.NAME)) {
            return instance;
         }
      }

      throw new RuntimeException(Constants.NAME + " not found. Run the CreateInstance example first.");
   }

   private void grantRootAccess(Instance instance) throws TimeoutException {
      System.out.println("Grant root access");
      
      String password = instanceApi.enableRoot(getInstance().getId()); // enable root on the instance
      
      System.out.println("  " + password);
   }

   /**
    * Always close your service when you're done with it.
    * @throws IOException 
    */
   public void close() throws IOException {
      Closeables.close(api, true);
   }
}
