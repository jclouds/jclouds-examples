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
package org.jclouds.examples.google.computeengine;

import static org.jclouds.compute.config.ComputeServiceProperties.POLL_INITIAL_PERIOD;
import static org.jclouds.compute.config.ComputeServiceProperties.POLL_MAX_PERIOD;
import static org.jclouds.compute.options.TemplateOptions.Builder.overrideLoginCredentials;
import static org.jclouds.examples.google.computeengine.Constants.POLL_PERIOD_TWENTY_SECONDS;
import static org.jclouds.examples.google.computeengine.Constants.PROVIDER;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.Statements;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.inject.Module;

/**
 * This example connects to an existing VM and runs a simple program (/bin/ls) on it.
 */
public class ExecuteCommand implements Closeable {
   private final ComputeService computeService;

   /**
    * Prerequisites:
    * - service account created and its private key available on the local machine (see README.txt).
    * - a VM created and SSH access enabled.
    *
    * The first argument (args[0]) is your service account email address
    *    (https://developers.google.com/console/help/new/#serviceaccounts).
    * The second argument (args[1]) is a path to your service account private key PEM file without a password
    *    (https://developers.google.com/console/help/new/#serviceaccounts).
    * The third argument (args[2]) is the name of your instance
    *    (https://developers.google.com/compute/docs/instances#start_vm).
    * The fourth argument (args[3]) is the zone where your instance is located
    *    (https://developers.google.com/compute/docs/zones).
    *
    * Example:
    *
    * java org.jclouds.examples.google.computeengine.ExecuteCommand \
    *    somecrypticname@developer.gserviceaccount.com \
    *    /home/planetnik/Work/Cloud/OSS/certificate/gcp-oss.pem \
    *    planetnik-main \
    *    europe-west1-a
    */
   public static void main(final String[] args) {
      String serviceAccountEmailAddress = args[0];
      String serviceAccountKey = null;
      try {
         serviceAccountKey = Files.toString(new File(args[1]), Charset.defaultCharset());
      } catch (IOException e) {
         System.err.println("Cannot open service account private key PEM file: " + args[1] + "\n" + e.getMessage());
         System.exit(1);
      }
      String instanceName = args[2];
      String zone = args[3];
      String userName = System.getProperty("user.name");
      String sshPrivateKeyFileName = System.getProperty("user.home") + File.separator + ".ssh" + File.separator
         + "google_compute_engine";
      String sshPrivateKey = null;
      try {
         sshPrivateKey = Files.toString(new File(sshPrivateKeyFileName), Charset.defaultCharset());
      } catch (IOException e) {
         System.err.println("Unable to load your SSH private key at " + sshPrivateKeyFileName
                + "\nIt is required to perform any operations on your machine via SSH.\n"
                + "See https://developers.google.com/compute/docs/instances#sshkeys for more details.\n"
                + e.getMessage());
         System.exit(1);
      }

      ExecuteCommand executeApplication = new ExecuteCommand(serviceAccountEmailAddress, serviceAccountKey);

      try {
         NodeMetadata instance = executeApplication.locateInstance(instanceName, zone);
         if (instance != null) {
            String publicAddress = instance.getPublicAddresses().iterator().next();
            System.out.format("Instance %s found with IP %s%n", instance.getName(), publicAddress);
         } else {
            System.err.format("Error: Instance %s could not be located in zone %s.%n", instanceName, zone);
            System.exit(1);
         }
         executeApplication.executeSimpleCommand(instance, userName, sshPrivateKey);
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         try {
            executeApplication.close();
         } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
         }
      }
   }

   public ExecuteCommand(final String serviceAccountEmailAddress, final String serviceAccountKey) {
      // These properties control how often jclouds polls for a status update.
      Properties overrides = new Properties();
      overrides.setProperty(POLL_INITIAL_PERIOD, POLL_PERIOD_TWENTY_SECONDS);
      overrides.setProperty(POLL_MAX_PERIOD, POLL_PERIOD_TWENTY_SECONDS);

      Iterable<Module> modules = ImmutableSet.<Module> of(new SshjSshClientModule());

      ComputeServiceContext context = ContextBuilder.newBuilder(PROVIDER)
            .credentials(serviceAccountEmailAddress, serviceAccountKey)
            .modules(modules)
            .overrides(overrides)
            .buildView(ComputeServiceContext.class);
      computeService = context.getComputeService();
   }

   private NodeMetadata locateInstance(final String instanceName, final String zone)
         throws RunNodesException, TimeoutException {
      System.out.format("Locating instance: %s%n", zone + "/" + instanceName);

      NodeMetadata instance = computeService.getNodeMetadata(zone + "/" + instanceName);

      return instance;
   }

   private void executeSimpleCommand(
         final NodeMetadata instance, final String googleUserName, final String sshPrivateKey) {
      Statement script = Statements.exec("ls -l /");

      // Set up credentials.
      RunScriptOptions options = overrideLoginCredentials((new LoginCredentials.Builder())
            .user(googleUserName)
            .privateKey(sshPrivateKey)
            .build())
          .blockOnComplete(true)
          .runAsRoot(false);

      ExecResponse response = computeService.runScriptOnNode(instance.getId(), script, options);

      System.out.format("Exit Status:%n============%n%s%n%n", response.getExitStatus());
      if (response.getExitStatus() == 0) {
         System.out.format("Output:%n======%n%s%n%n", response.getOutput());
      } else {
         System.out.format("Error:%n======%n%s%n%n", response.getError());
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public final void close() throws IOException {
      Closeables.close(computeService.getContext(), true);
   }
}
