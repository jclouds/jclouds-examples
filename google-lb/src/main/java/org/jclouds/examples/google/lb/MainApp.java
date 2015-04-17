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

package org.jclouds.examples.google.lb;


import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.domain.Credentials;
import org.jclouds.googlecloud.GoogleCredentialsFromJson;
import org.jclouds.googlecomputeengine.GoogleComputeEngineApi;
import org.jclouds.googlecomputeengine.GoogleComputeEngineProviderMetadata;
import org.jclouds.googlecomputeengine.domain.Firewall;
import org.jclouds.googlecomputeengine.domain.ForwardingRule;
import org.jclouds.googlecomputeengine.domain.Metadata;
import org.jclouds.googlecomputeengine.domain.NewInstance;
import org.jclouds.googlecomputeengine.domain.Operation;
import org.jclouds.googlecomputeengine.features.InstanceApi;
import org.jclouds.googlecomputeengine.features.OperationApi;
import org.jclouds.googlecomputeengine.options.FirewallOptions;
import org.jclouds.googlecomputeengine.options.ForwardingRuleCreationOptions;
import org.jclouds.googlecomputeengine.options.HttpHealthCheckCreationOptions;
import org.jclouds.googlecomputeengine.options.TargetPoolCreationOptions;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.inject.Injector;

/**
 * Demonstrates the use of {@link ComputeService}.
 * <p/>
 * Usage is:
 * {@code java MainApp provider identity credential groupName (add|exec|run|destroy)}
 * if {@code exec} is used, the following parameter is a command, which should
 * be passed in quotes
 * if {@code run} is used, the following parameter is a file to execute.
 */
public class MainApp {

   public static enum Action {
      CREATE, REQUEST, DESTROY, DELETE_STARTUP_SCRIPT
   }

   public static String DEFAULT_ZONE = "us-central1-b";
   public static String DEFAULT_REGION = "us-central1";
   public static String DEFAULT_MACHINE_TYPE = "n1-standard-1";
   public static URI DEFAULT_IMAGE_URL = URI.create("https://www.googleapis.com/compute/v1/projects/debian-cloud/global/images/backports-debian-7-wheezy-v20150325");
   public static String FIREWALL_TAG = "allow-tcp-port-80";
   public static String STARTUP_SCRIPT = "apt-get update && apt-get install -y apache2 && hostname > /var/www/index.html";

   public static int NUM_INSTANCES = 3;

   public static int PARAMETERS = 2;
   public static String INVALID_SYNTAX = "Invalid number of parameters. Syntax is: json_key_path (create|request|destroy|delete_startup_script)";

   public static void main(String[] args) {
      if (args.length < PARAMETERS)
         throw new IllegalArgumentException(INVALID_SYNTAX);

      String jsonKeyFile = args[0];
      Action action = Action.valueOf(args[1].toUpperCase());

      // Read in JSON key.
      String fileContents = null;
      try {
         fileContents = Files.toString(new File(jsonKeyFile), Charset.defaultCharset());
      } catch (IOException ex){
         System.out.println("Error Reading the Json key file. Please check the provided path is correct.");
         System.exit(1);
      }

      Supplier<Credentials> credentialSupplier = new GoogleCredentialsFromJson(fileContents);

      // This demonstrates how to initialize a ComputeServiceContext using json key files.
      ComputeServiceContext context = ContextBuilder.newBuilder("google-compute-engine")
            .credentialsSupplier(credentialSupplier)
            .buildView(ComputeServiceContext.class);

      Credentials credentials = credentialSupplier.get();

      GoogleComputeEngineApi googleApi = createGoogleComputeEngineApi(credentials.identity, credentials.credential);

      String project_name = googleApi.project().get().name();
      System.out.printf("Sucessfully Authenticated to project %s\n", project_name);

      InstanceApi instanceApi = googleApi.instancesInZone(DEFAULT_ZONE);

      switch (action) {
      case CREATE:
      {
         Metadata metadata = googleApi.project().get().commonInstanceMetadata();
         if (metadata.get("startup-script") != null && metadata.get("startup-script") != STARTUP_SCRIPT){

            System.out.println("Project already has a startup script, exiting to avoid overwriting it.");
            System.exit(1);
         }
         System.out.println("Updating startup script");
         metadata.put("startup-script", STARTUP_SCRIPT);
         Operation operation = googleApi.project().setCommonInstanceMetadata(metadata);
         OperationApi operationsApi = googleApi.operations();
         WaitForOperation(operationsApi, operation);

         URI networkURL = googleApi.networks().get("default").selfLink();
         if (networkURL == null){
            System.out.println("Your project does not have a default network. Please recreate the default network or try again with a new project");
            System.exit(1);
         }
         System.out.println("Creating:");

         // Add firewall rule to allow TCP on port 80
         FirewallOptions options = new FirewallOptions()
            .addAllowedRule(Firewall.Rule.create("tcp", ImmutableList.of("80"))).sourceRanges(ImmutableList.of("0.0.0.0/0"));
            //.addTargetTag(FIREWALL_TAG);
         operation = googleApi.firewalls().createInNetwork("jclouds-lb-firewall-tcp-80", networkURL, options);
         System.out.println(" - firewall");
         WaitForOperation(operationsApi, operation);

         URI machineTypeURL = googleApi.machineTypesInZone(DEFAULT_ZONE).get(DEFAULT_MACHINE_TYPE).selfLink();

         // Make requests to create instances.
         ArrayList<Operation> operations = new ArrayList<Operation>();
         for (int i = 0; i < NUM_INSTANCES; i++){
            Operation o = instanceApi.create(NewInstance.create("jclouds-lb-instance-" + i, machineTypeURL, networkURL, DEFAULT_IMAGE_URL));
            System.out.println(" - instance");
            operations.add(o);
         }

         ArrayList<URI> instances = new ArrayList<URI>();
         for (Operation op : operations){
            WaitForOperation(operationsApi, op);
            instances.add(op.targetLink());
         }

         // Create Health Check
         HttpHealthCheckCreationOptions healthCheckOptions = new HttpHealthCheckCreationOptions.Builder()
            .checkIntervalSec(1)
            .timeoutSec(1)
            .buildWithDefaults();
         operation = googleApi.httpHeathChecks().insert("jclouds-lb-healthcheck", healthCheckOptions);
         System.out.println(" - http health check");
         WaitForOperation(operationsApi, operation);
         URI healthCheckURI = googleApi.httpHeathChecks().get("jclouds-lb-healthcheck").selfLink();

         // Create Target Pool
         TargetPoolCreationOptions targetPoolOptions = new TargetPoolCreationOptions.Builder("jclouds-lb-target-pool")
            .healthChecks(ImmutableList.of(healthCheckURI))
            .instances(instances).build();
         Operation targetPoolOperation = googleApi.targetPoolsInRegion(DEFAULT_REGION).create(targetPoolOptions);
         System.out.println(" - target pool");
         WaitForOperation(operationsApi, targetPoolOperation);

         // Create Forwarding Rule
         ForwardingRuleCreationOptions forwardingOptions = new ForwardingRuleCreationOptions.Builder()
            .ipProtocol(ForwardingRule.IPProtocol.TCP)
            .target(targetPoolOperation.targetLink())
            .build();
         operation = googleApi.forwardingRulesInRegion(DEFAULT_REGION).create("jclouds-lb-forwarding", forwardingOptions);
         System.out.println(" - forwarding rule");
         WaitForOperation(operationsApi, operation);

         String ipAddress = googleApi.forwardingRulesInRegion(DEFAULT_REGION).get("jclouds-lb-forwarding").ipAddress();
         System.out.println("Ready to recieve traffic at " + ipAddress);

         break;
      }
      case REQUEST:
      {
         // Find the created forwarding rule.
         ForwardingRule forwardingRule = googleApi.forwardingRulesInRegion(DEFAULT_REGION).get("jclouds-lb-forwarding");
         if (forwardingRule == null) {
            System.out.println("jclouds-lb-forwarding rule does not exist. Have you successfully run create?");
            System.exit(1);
         }
         String ipAddress = googleApi.forwardingRulesInRegion(DEFAULT_REGION).get("jclouds-lb-forwarding").ipAddress();
         System.out.printf("Found the forwarding rule! Try executing 'while true; do curl -m1 %s; done'\n", ipAddress);

         break;
      }
      case DELETE_STARTUP_SCRIPT:
      {
         System.out.println("removing startup script from project metadata");
         DeleteStartupScript(googleApi);
         break;
      }
      case DESTROY:
      {
         // Delete Forwarding Rule
         googleApi.forwardingRulesInRegion(DEFAULT_REGION).delete("jclouds-lb-forwarding");

         // Delete Target Pool
         googleApi.targetPoolsInRegion(DEFAULT_REGION).delete("jclouds-lb-target-pool");

         // Delete Health Check
         googleApi.httpHeathChecks().delete("jclouds-lb-healthcheck");

         // Delete Instances
         ArrayList<Operation> operations = new ArrayList<Operation>();
         for (int i = 0; i < NUM_INSTANCES; i++){
            Operation o = instanceApi.delete("jclouds-lb-instance-" + i);
            operations.add(o);
         }

         // Delete Firewall Rule
         googleApi.firewalls().delete("jclouds-lb-firewall-tcp-80");

         // Delete Startup Script
         DeleteStartupScript(googleApi);

         System.out.println("ran cleanup");
         break;
      }
      }
   }

   public static void DeleteStartupScript(GoogleComputeEngineApi googleApi){
      Metadata metadata = googleApi.project().get().commonInstanceMetadata();
      metadata.remove("startup-script");
      googleApi.project().setCommonInstanceMetadata(metadata);
   }

   public static int WaitForOperation(OperationApi api, Operation operation){
      int timeout = 60; // seconds
      int time = 0;

      while (operation.status() != Operation.Status.DONE){
         if (time >= timeout){
            return 1;
         }
         time++;
         try {
            Thread.sleep(1000);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }

         operation = api.get(operation.selfLink());
      }
      //TODO: Check for errors.
      return 0;
   }

   // This function demonstrates how to get an instance of the GoogleComputeEngineApi.
   private static GoogleComputeEngineApi createGoogleComputeEngineApi(String identity, String credential){
      ContextBuilder contextBuilder = ContextBuilder.newBuilder(GoogleComputeEngineProviderMetadata.builder().build())
            .credentials(identity, credential);
      Injector injector = contextBuilder.buildInjector();
      return injector.getInstance(GoogleComputeEngineApi.class);
   }
}
