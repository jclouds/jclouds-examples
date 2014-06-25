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

import static org.jclouds.examples.rackspace.clouddatabases.Constants.*;

/**
 * This example creates a Cloud Databases instance.
 * This instance will be used to run a database later on in the Create Database example.
 */
public class CreateInstance implements Closeable {
   private final TroveApi troveApi;
   private final FlavorApi flavorApi;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument  (args[0]) must be your username.
    * The second argument (args[1]) must be your API key.
    */
   public static void main(String[] args) throws IOException {
      CreateInstance createInstance = new CreateInstance(args[0], args[1]);

      try {
         Flavor flavor = createInstance.getFlavor();
         createInstance.createInstance(flavor);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createInstance.close();
      }
   }

   public CreateInstance(String username, String apiKey) {
      troveApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(TroveApi.class);

      flavorApi = troveApi.getFlavorApiForZone(ZONE);
   }

   /**
    * @return Flavor The first Flavor available.
    */
   private Flavor getFlavor() {
      return Iterables.getFirst(flavorApi.list(), null);
   }

   private void createInstance(Flavor flavor) throws TimeoutException {
      System.out.format("Create Instance for Flavor: %s%n", flavor.getId());

      TroveUtils utils = new TroveUtils(troveApi);
      // This call will take a while - it ensures a working instance is created.
      Instance instance = utils.getWorkingInstance(ZONE, NAME, "" + flavor.getId(), 1);

      System.out.format("  %s%n", instance);
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
