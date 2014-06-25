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
import java.util.List;

import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.autoscale.v1.AutoscaleApi;
import org.jclouds.rackspace.autoscale.v1.domain.Group;
import org.jclouds.rackspace.autoscale.v1.domain.GroupConfiguration;
import org.jclouds.rackspace.autoscale.v1.domain.LaunchConfiguration;
import org.jclouds.rackspace.autoscale.v1.domain.LaunchConfiguration.LaunchConfigurationType;
import org.jclouds.rackspace.autoscale.v1.domain.LoadBalancer;
import org.jclouds.rackspace.autoscale.v1.domain.Personality;
import org.jclouds.rackspace.autoscale.v1.domain.CreateScalingPolicy;
import org.jclouds.rackspace.autoscale.v1.domain.CreateScalingPolicy.ScalingPolicyTargetType;
import org.jclouds.rackspace.autoscale.v1.domain.CreateScalingPolicy.ScalingPolicyType;
import org.jclouds.rackspace.autoscale.v1.features.GroupApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;

/**
 * This example creates a Scaling Policy in a Scaling Group.
 *
 * The scaling group contains a set of scaling policies.
 * Each scaling policy can have webhooks associated to it.
 * Webhooks can be used to execute scaling policies.
 */
public class CreatePolicy implements Closeable {
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
      CreatePolicy createPolicy = new CreatePolicy(args[0], args[1]);

      try {
         createPolicy.createPolicy();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createPolicy.close();
      }
   }

   public CreatePolicy(String username, String apiKey) {
      autoscaleApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(AutoscaleApi.class);

      groupApi = autoscaleApi.getGroupApiForZone(ZONE);
   }

   private void createPolicy() {
      System.out.format("Create Autoscale Group%n");

      GroupConfiguration groupConfiguration = GroupConfiguration.builder()
            .maxEntities(5)
            .cooldown(2)
            .name(NAME)
            .minEntities(0)
            .metadata(ImmutableMap.of("notes","This is an autoscale group for examples"))
            .build();

      LaunchConfiguration launchConfiguration = LaunchConfiguration.builder()
            .loadBalancers(ImmutableList.of(LoadBalancer.builder().port(8080).id(9099).build()))
            .serverName(NAME)
            .serverImageRef("0d589460-f177-4b0f-81c1-8ab8903ac7d8")
            .serverFlavorRef("2")
            .serverDiskConfig("AUTO")
            .serverMetadata(ImmutableMap.of("notes","Server examples notes"))
            .networks(ImmutableList.<String>of("internal", "public"))
            .personalities(ImmutableList.of(Personality.builder().path("filepath").contents("VGhpcyBpcyBhIHRlc3QgZmlsZS4=").build()))
            .type(LaunchConfigurationType.LAUNCH_SERVER)
            .build();

      List<CreateScalingPolicy> scalingPolicies = Lists.newArrayList();

      CreateScalingPolicy scalingPolicy = CreateScalingPolicy.builder()
            .cooldown(0)
            .type(ScalingPolicyType.WEBHOOK)
            .name(NAME)
            .targetType(ScalingPolicyTargetType.PERCENT_CHANGE)
            .target("1")
            .build();
      scalingPolicies.add(scalingPolicy);

      Group g = groupApi.create(groupConfiguration, launchConfiguration, scalingPolicies);

      System.out.format("  %s%n", g.toString());
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
