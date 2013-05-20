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

package org.jclouds.examples.ec2.windows;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.jclouds.aws.ec2.AWSEC2Client;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ec2.compute.domain.PasswordDataAndPrivateKey;
import org.jclouds.ec2.compute.functions.WindowsLoginCredentialsFromEncryptedData;
import org.jclouds.ec2.domain.PasswordData;
import org.jclouds.logging.Logger;
import org.jclouds.predicates.RetryablePredicate;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * FIXME
 *
 * @author Richard Downer
 */
public class WindowsInstanceStarter {
   private final Arguments arguments;
   private final Logger logger;
   private final ComputeServiceContext context;
   private final AWSEC2Client ec2Client;
   private final ComputeService computeService;

   public WindowsInstanceStarter(Arguments arguments, ComputeServiceContext context) {
      this.arguments = arguments;
      this.context = context;

      logger = context.getUtils().getLoggerFactory().getLogger(WindowsInstanceStarter.class.getName());
      ec2Client = AWSEC2Client.class.cast(context.getProviderSpecificContext().getApi());
      computeService = context.getComputeService();
   }

   public void run() {
      final String region = arguments.getRegion();

      // Build a template
      Template template = computeService.templateBuilder()
         .locationId(region)
         .imageNameMatches(arguments.getImageNamePattern())
         .hardwareId(arguments.getInstanceType())
         .build();
      logger.info("Selected AMI is: %s", template.getImage().toString());
      template.getOptions().inboundPorts(3389);

      // Create the node
      logger.info("Creating node and waiting for it to become available");
      Set<? extends NodeMetadata> nodes = null;
      try {
         nodes = computeService.createNodesInGroup("basic-ami", 1, template);
      } catch (RunNodesException e) {
         logger.error(e, "Unable to start nodes; aborting");
         return;
      }
      NodeMetadata node = Iterables.getOnlyElement(nodes);

      // Wait for the administrator password
      logger.info("Waiting for administrator password to become available");

      // This predicate will call EC2's API to get the Windows Administrator
      // password, and returns true if there is password data available.
      Predicate<String> passwordReady = new Predicate<String>() {
         @Override
         public boolean apply(@Nullable String s) {
            if (Strings.isNullOrEmpty(s)) return false;
            PasswordData data = ec2Client.getWindowsServices().getPasswordDataInRegion(region, s);
            if (data == null) return false;
            return !Strings.isNullOrEmpty(data.getPasswordData());
         }
      };

      // Now wait, using RetryablePredicate
      final int maxWait = 600;
      final int period = 10;
      final TimeUnit timeUnit = TimeUnit.SECONDS;
      RetryablePredicate<String> passwordReadyRetryable = new RetryablePredicate<String>(passwordReady, maxWait, period, timeUnit);
      boolean isPasswordReady = passwordReadyRetryable.apply(node.getProviderId());
      if (!isPasswordReady) {
         logger.error("Password is not ready after %s %s - aborting and shutting down node", maxWait, timeUnit.toString());
         computeService.destroyNode(node.getId());
         return;
      }

      // Now we can get the password data, decrypt it, and get a LoginCredentials instance
      PasswordDataAndPrivateKey dataAndKey = new PasswordDataAndPrivateKey(
         ec2Client.getWindowsServices().getPasswordDataInRegion(region, node.getProviderId()),
         node.getCredentials().getPrivateKey());
      WindowsLoginCredentialsFromEncryptedData f = context.getUtils().getInjector().getInstance(WindowsLoginCredentialsFromEncryptedData.class);
      LoginCredentials credentials = f.apply(dataAndKey);

      // Send to the log the details you need to log in to the instance with RDP
      String publicIp = Iterables.getFirst(node.getPublicAddresses(), null);
      logger.info("IP address: %s", publicIp);
      logger.info("Login name: %s", credentials.getUser());
      logger.info("Password:   %s", credentials.getPassword());

      // Wait for Enter on the console
      logger.info("Hit Enter to shut down the node.");
      InputStreamReader converter = new InputStreamReader(System.in);
      BufferedReader in = new BufferedReader(converter);
      try {
         in.readLine();
      } catch (IOException e) {
         logger.error(e, "IOException while reading console input");
      }

      // Tidy up
      logger.info("Shutting down");
      computeService.destroyNode(node.getId());
   }
}
