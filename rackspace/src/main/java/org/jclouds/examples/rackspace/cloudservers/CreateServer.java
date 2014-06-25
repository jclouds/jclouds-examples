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
package org.jclouds.examples.rackspace.cloudservers;

import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static org.jclouds.compute.config.ComputeServiceProperties.POLL_INITIAL_PERIOD;
import static org.jclouds.compute.config.ComputeServiceProperties.POLL_MAX_PERIOD;
import static org.jclouds.examples.rackspace.cloudservers.Constants.*;

/**
 * This example creates an Ubuntu 12.04 server with 1024 MB of RAM on the Rackspace Cloud.
 *
 */
public class CreateServer implements Closeable {
   private final ComputeService computeService;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      CreateServer createServer = new CreateServer(args[0], args[1]);

      try {
         createServer.createServer();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createServer.close();
      }
   }

   public CreateServer(String username, String apiKey) {
      // These properties control how often jclouds polls for a status update
      Properties overrides = new Properties();
      overrides.setProperty(POLL_INITIAL_PERIOD, POLL_PERIOD_TWENTY_SECONDS);
      overrides.setProperty(POLL_MAX_PERIOD, POLL_PERIOD_TWENTY_SECONDS);

      ComputeServiceContext context = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .overrides(overrides)
            .buildView(ComputeServiceContext.class);
      computeService = context.getComputeService();
   }

   /**
    * Create a server based on a Template. This method uses Template.fromHardware() and Template.fromImage() to
    * also demonstrate iterating through Hardware and Images. Alternatively you do the same without iterating
    * using the following Template.
    *
    * Template template = computeService.templateBuilder()
    *     .locationId(getLocationId())
    *     .osFamily(OsFamily.UBUNTU)
    *     .osVersionMatches("12.04")
    *     .minRam(1024)
    *     .build();
    */
   private void createServer() throws RunNodesException, TimeoutException {
      System.out.format("Create Server%n");

      Template template = computeService.templateBuilder()
            .locationId(ZONE)
            .fromHardware(getHardware())
            .fromImage(getImage())
            .build();

      // This method will continue to poll for the server status and won't return until this server is ACTIVE
      // If you want to know what's happening during the polling, enable logging. See
      // /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      Set<? extends NodeMetadata> nodes = computeService.createNodesInGroup(NAME, 1, template);

      NodeMetadata nodeMetadata = nodes.iterator().next();
      String publicAddress = nodeMetadata.getPublicAddresses().iterator().next();

      System.out.format("  %s%n", nodeMetadata);
      System.out.format("  Login: ssh %s@%s%n", nodeMetadata.getCredentials().getUser(), publicAddress);
      System.out.format("  Password: %s%n", nodeMetadata.getCredentials().getPassword());
   }

   /**
    * This method uses the generic ComputeService.listHardwareProfiles() to find the hardware profile.
    *
    * @return The Hardware with 1024 MB of RAM
    */
   private Hardware getHardware() {
      System.out.format("  Hardware Profiles (Flavors)%n");

      Set<? extends Hardware> profiles = computeService.listHardwareProfiles();
      Hardware result = null;

      for (Hardware profile: profiles) {
         System.out.format("    %s%n", profile);
         if (profile.getProviderId().equals("performance1-1")) {
            result = profile;
         }
      }

      if (result == null) {
         System.err.println("Performance 1-1 flavor not found. Using first flavor found.%n");
         result = profiles.iterator().next();
      }

      return result;
   }

   /**
    * This method uses the generic ComputeService.listImages() to find the image.
    *
    * @return An Ubuntu 12.04 Image
    */
   private Image getImage() {
      System.out.format("  Images%n");

      Set<? extends Image> images = computeService.listImages();
      Image result = null;

      for (Image image: images) {
         System.out.format("    %s%n", image);
         if (image.getOperatingSystem().getName().equals("Ubuntu 12.04 LTS (Precise Pangolin)")) {
            result = image;
         }
      }

      if (result == null) {
         System.err.println("Image with Ubuntu 12.04 operating system not found. Using first image found.%n");
         result = images.iterator().next();
      }

      return result;
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(computeService.getContext(), true);
   }
}
