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
package org.jclouds.examples.rackspace.autoscale;

import static org.jclouds.examples.rackspace.autoscale.Constants.NAME;
import static org.jclouds.examples.rackspace.autoscale.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.autoscale.Constants.ZONE;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.autoscale.v1.AutoscaleApi;
import org.jclouds.rackspace.autoscale.v1.domain.GroupState;
import org.jclouds.rackspace.autoscale.v1.domain.CreateScalingPolicy;
import org.jclouds.rackspace.autoscale.v1.domain.CreateScalingPolicy.ScalingPolicyTargetType;
import org.jclouds.rackspace.autoscale.v1.domain.CreateScalingPolicy.ScalingPolicyType;
import org.jclouds.rackspace.autoscale.v1.domain.ScalingPolicy;
import org.jclouds.rackspace.autoscale.v1.features.GroupApi;
import org.jclouds.rackspace.autoscale.v1.features.PolicyApi;

import com.google.common.io.Closeables;
import com.google.common.util.concurrent.Uninterruptibles;

/**
 * This code cleans up the autoscale examples and can be used as an example for cleanup and delete.
 * Note that you need to cleanup the group before you can delete it.
 */
public class AutoscaleCleanup implements Closeable {
   private final AutoscaleApi autoscaleApi;
   private final GroupApi groupApi;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      AutoscaleCleanup autoscaleCleanup = new AutoscaleCleanup(args[0], args[1]);

      try {
         autoscaleCleanup.autoscaleCleanup();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         autoscaleCleanup.close();
      }
   }

   public AutoscaleCleanup(String username, String apiKey) {
      autoscaleApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(AutoscaleApi.class);

      groupApi = autoscaleApi.getGroupApiForZone(ZONE);
   }

   private void autoscaleCleanup() {
      System.out.format("Cleanup autoscale %n");

      // Remove ALL policies and groups with that name
      for (GroupState g : groupApi.listGroupStates()) {
         PolicyApi pa = autoscaleApi.getPolicyApiForZoneAndGroup(ZONE, g.getId());
         for(ScalingPolicy p : pa.list()) {
            if(p.getName().equals(NAME)) {
               System.out.format("Found matching policy: %s with cooldown %s%n", p.getId(), p.getCooldown());
               String policyId = p.getId();

               if (!(p.getTarget().equals("0") && p.getTargetType().equals(ScalingPolicyTargetType.DESIRED_CAPACITY))) {
                  System.out.format("Removing servers %n");

                  // Update policy to 0 servers
                  CreateScalingPolicy scalingPolicy = CreateScalingPolicy.builder()
                        .cooldown(3)
                        .type(ScalingPolicyType.WEBHOOK)
                        .name(NAME)
                        .targetType(ScalingPolicyTargetType.DESIRED_CAPACITY)
                        .target("0")
                        .build();

                  pa.update(policyId, scalingPolicy);
                  Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);

                  try {
                     pa.execute(policyId);
                  } catch (Exception e) {
                     // This will fail to execute when the number of servers is already zero (no change).
                  }
               }
               Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
               pa.delete(policyId);
               groupApi.delete(g.getId());
            } else {
               System.out.format("Found another policy: %s - %s with cooldown %s%n", p.getName(), p.getId(), p.getCooldown());
            }
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
      Closeables.close(autoscaleApi, true);
   }
}
