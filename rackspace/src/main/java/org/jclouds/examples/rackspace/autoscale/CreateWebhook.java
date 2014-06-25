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
import org.jclouds.rackspace.autoscale.v1.domain.Webhook;
import org.jclouds.rackspace.autoscale.v1.features.GroupApi;
import org.jclouds.rackspace.autoscale.v1.features.PolicyApi;
import org.jclouds.rackspace.autoscale.v1.features.WebhookApi;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Closeables;

/**
 * This example creates a webhook for the Scaling Policy.
 */
public class CreateWebhook implements Closeable {
   private final AutoscaleApi autoscaleApi;
   private final GroupApi groupApi;
   private final PolicyApi policyApi;
   private final WebhookApi webhookApi;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      CreateWebhook createWebhook = new CreateWebhook(args[0], args[1]);

      try {
         createWebhook.createWebhook();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createWebhook.close();
      }
   }

   public CreateWebhook(String username, String apiKey) {
      autoscaleApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(AutoscaleApi.class);

      groupApi = autoscaleApi.getGroupApiForZone(ZONE);
      String groupId = Utils.getGroupId(groupApi);
      policyApi = autoscaleApi.getPolicyApiForZoneAndGroup(ZONE, groupId);
      String policyId = Utils.getPolicyId(policyApi);
      webhookApi = autoscaleApi.getWebhookApiForZoneAndGroupAndPolicy(ZONE, groupId, policyId);
   }

   private void createWebhook() {
      System.out.format("Create Webhook%n");

      FluentIterable<Webhook> result = webhookApi.create(NAME, ImmutableMap.<String, Object>of());

      System.out.format("  %s%n", result.first().get());
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
