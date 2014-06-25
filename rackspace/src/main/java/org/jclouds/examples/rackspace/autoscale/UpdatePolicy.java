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

import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.autoscale.v1.AutoscaleApi;
import org.jclouds.rackspace.autoscale.v1.domain.CreateScalingPolicy;
import org.jclouds.rackspace.autoscale.v1.domain.CreateScalingPolicy.ScalingPolicyTargetType;
import org.jclouds.rackspace.autoscale.v1.domain.CreateScalingPolicy.ScalingPolicyType;
import org.jclouds.rackspace.autoscale.v1.features.GroupApi;
import org.jclouds.rackspace.autoscale.v1.features.PolicyApi;

import com.google.common.io.Closeables;

/**
 * This example updates a Scaling Policy in a Scaling Group.
 */
public class UpdatePolicy implements Closeable {
   private final AutoscaleApi autoscaleApi;
   private final GroupApi groupApi;
   private final PolicyApi policyApi;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      UpdatePolicy updatePolicy = new UpdatePolicy(args[0], args[1]);

      try {
         updatePolicy.updatePolicy();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         updatePolicy.close();
      }
   }

   public UpdatePolicy(String username, String apiKey) {
      autoscaleApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(AutoscaleApi.class);

      groupApi = autoscaleApi.getGroupApiForZone(ZONE);
      String groupId = Utils.getGroupId(groupApi);
      policyApi = autoscaleApi.getPolicyApiForZoneAndGroup(ZONE, groupId);
   }

   private void updatePolicy() {
      System.out.format("Update autoscale policy%n");

      String policyId = policyApi.list().first().get().getId();

      CreateScalingPolicy scalingPolicy = CreateScalingPolicy.builder()
            .cooldown(3)
            .type(ScalingPolicyType.WEBHOOK)
            .name(NAME)
            .targetType(ScalingPolicyTargetType.INCREMENTAL)
            .target("1")
            .build();

      boolean result = policyApi.update(policyId, scalingPolicy);

      System.out.format("  %s%n", result);
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
