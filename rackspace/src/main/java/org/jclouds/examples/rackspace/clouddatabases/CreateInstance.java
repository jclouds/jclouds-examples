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
import org.jclouds.openstack.trove.v1.domain.Flavor;
import org.jclouds.openstack.trove.v1.domain.Instance;
import org.jclouds.openstack.trove.v1.features.FlavorApi;
import org.jclouds.openstack.trove.v1.utils.TroveUtils;

import com.google.common.collect.Iterables;
import com.google.common.io.Closeables;

/**
 * This example creates a Cloud Databases instance. 
 * This instance will be used to run a database later on in the Create Database example.
 * 
 * @author Zack Shoylev
 */
public class CreateInstance implements Closeable {
   private TroveApi api;
   private FlavorApi flavorApi;

   /**
    * To get a username and API key see 
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument  (args[0]) must be your username.
    * The second argument (args[1]) must be your API key.
    * @throws IOException 
    */
   public static void main(String[] args) throws IOException {
      
      CreateInstance createInstance = new CreateInstance();

      try {
         createInstance.init(args);
         Flavor flavor = createInstance.getFlavor();
         createInstance.createInstance(flavor);
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         createInstance.close();
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
      
      flavorApi = api.getFlavorApiForZone(Constants.ZONE);
   }

   /**
    * @return Flavor The first Flavor available.
    */
   private Flavor getFlavor() {
      return Iterables.getFirst(flavorApi.list(), null);
   }

   private void createInstance(Flavor flavor) throws TimeoutException {
      System.out.println("Create Instance for flavor: " + flavor.getId());

      TroveUtils utils = new TroveUtils(api);
      // This call will take a while - it ensures a working instance is created.
      Instance instance = utils.getWorkingInstance(Constants.ZONE, Constants.NAME, "" + flavor.getId(), 1);

      System.out.println("  " + instance);
   }

   /**
    * Always close your service when you're done with it.
    * @throws IOException 
    */
   public void close() throws IOException {
      Closeables.close(api, true);
   }
}
