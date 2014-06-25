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

import static org.jclouds.examples.rackspace.autoscale.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.autoscale.Constants.ZONE;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.autoscale.v1.AutoscaleApi;
import org.jclouds.rackspace.autoscale.v1.domain.Webhook;
import org.jclouds.rackspace.autoscale.v1.features.GroupApi;
import org.jclouds.rackspace.autoscale.v1.features.PolicyApi;
import org.jclouds.rackspace.autoscale.v1.features.WebhookApi;
import org.jclouds.rackspace.autoscale.v1.utils.AutoscaleUtils;

import com.google.common.io.Closeables;
import com.google.common.util.concurrent.Uninterruptibles;

/**
 * This example executes a scaling policy in two ways:
 * - Authenticated API call using jclouds.
 * - Anonymously using just the webhook URL.
 */
public class ExecuteWebhook implements Closeable {
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
      ExecuteWebhook executeWebhook = new ExecuteWebhook(args[0], args[1]);

      try {
         executeWebhook.executeWebhook();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         executeWebhook.close();
      }
   }

   public ExecuteWebhook(String username, String apiKey) {
      autoscaleApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(AutoscaleApi.class);

      groupApi = autoscaleApi.getGroupApiForZone(ZONE);
      String groupId = Utils.getGroupId(groupApi);
      policyApi = autoscaleApi.getPolicyApiForZoneAndGroup(ZONE, groupId);
      String policyId = Utils.getPolicyId(policyApi);
      webhookApi = autoscaleApi.getWebhookApiForZoneAndGroupAndPolicy(ZONE, groupId, policyId);
   }

   private void executeWebhook() {
      System.out.format("Execute Webhook%n");

      String policyId = Utils.getPolicyId(policyApi);

      Uninterruptibles.sleepUninterruptibly(10, TimeUnit.SECONDS);
      boolean result = policyApi.execute(policyId);

      System.out.format("  %s%n", result);

      System.out.format("Execute Webhook - again, anonymously%n");

      Webhook webhook = webhookApi.list().first().get();
      try {
         result = AutoscaleUtils.execute(webhook.getAnonymousExecutionURI().get());
      } catch (IOException e) {
         e.printStackTrace();
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
      Closeables.close(autoscaleApi, true);
   }
}
