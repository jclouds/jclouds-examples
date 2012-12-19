/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
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

import static com.google.common.io.Closeables.closeQuietly;

import java.io.Closeable;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.config.ComputeServiceProperties;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.domain.Location;

/**
 * This example creates an Ubuntu 12.04 server with 512 MB of RAM on the Rackspace Cloud. 
 *  
 * @author Everett Toews
 */
public class CreateServer implements Closeable {
   private ComputeService compute;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      CreateServer createServer = new CreateServer();

      try {
         createServer.init(args);
         createServer.createServer();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createServer.close();
      }
   }

   private void init(String[] args) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the provider to "rackspace-cloudservers-uk"
      String provider = "rackspace-cloudservers-us";

      String username = args[0];
      String apiKey = args[1];

      // These properties control how often jclouds polls for a status udpate
      Properties overrides = new Properties();
      overrides.setProperty(ComputeServiceProperties.POLL_INITIAL_PERIOD, Constants.POLL_PERIOD_TWENTY_SECONDS);
      overrides.setProperty(ComputeServiceProperties.POLL_MAX_PERIOD, Constants.POLL_PERIOD_TWENTY_SECONDS);

      ComputeServiceContext context = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .overrides(overrides)
            .buildView(ComputeServiceContext.class);
      compute = context.getComputeService();
   }

   /**
    * Create a server based on a Template. This method uses Template.fromHardware() and Template.fromImage() to
    * also demonstrate iterating through Hardware and Images. Alternatively you do the same without iterating
    * using the following Template.
    * 
    * Template template = compute.templateBuilder()
    *     .locationId(getLocationId())
    *     .osFamily(OsFamily.UBUNTU)
    *     .osVersionMatches("12.04")
    *     .minRam(512)
    *     .build();
    */
   private void createServer() throws RunNodesException, TimeoutException {
      Template template = compute.templateBuilder()
            .locationId(getLocationId())
            .fromHardware(getHardware())
            .fromImage(getImage())
            .build();

      System.out.println("Create Server");

      // This method will continue to poll for the server status and won't return until this server is ACTIVE
      // If you want to know what's happening during the polling, enable logging. See
      // /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      Set<? extends NodeMetadata> nodes = compute.createNodesInGroup(Constants.NAME, 1, template);

      NodeMetadata nodeMetadata = nodes.iterator().next();
      String publicAddress = nodeMetadata.getPublicAddresses().iterator().next();

      System.out.println("  " + nodeMetadata);
      System.out.println("  Login: ssh " + nodeMetadata.getCredentials().getUser() + "@" + publicAddress);
      System.out.println("  Password: " + nodeMetadata.getCredentials().getPassword());
   }

   /**
    * This method uses the generic ComputeService.listAssignableLocations() to find the location.
    * 
    * @return The first available Location
    */
   private String getLocationId() {
      System.out.println("Locations");

      Set<? extends Location> locations = compute.listAssignableLocations();

      for (Location location: locations) {
         System.out.println("  " + location);
      }

      return locations.iterator().next().getId();
   }

   /**
    * This method uses the generic ComputeService.listHardwareProfiles() to find the hardware profile.
    * 
    * @return The Hardware with 512 MB of RAM
    */
   private Hardware getHardware() {
      System.out.println("Hardware Profiles (Flavors)");

      Set<? extends Hardware> profiles = compute.listHardwareProfiles();
      Hardware result = null;

      for (Hardware profile: profiles) {
         System.out.println("  " + profile);
         if (profile.getRam() == 512) {
            result = profile;
         }
      }

      if (result == null) {
         System.err.println("Flavor with 512 MB of RAM not found. Using first flavor found.");
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
      System.out.println("Images");

      Set<? extends Image> images = compute.listImages();
      Image result = null;

      for (Image image: images) {
         System.out.println("  " + image);
         if (image.getOperatingSystem().getName().equals("Ubuntu 12.04 LTS (Precise Pangolin)")) {
            result = image;
         }
      }

      if (result == null) {
         System.err.println("Image with Ubuntu 12.04 operating system not found. Using first image found.");
         result = images.iterator().next();
      }

      return result;
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(compute.getContext());
   }
}
