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

import com.google.common.io.Closeables;
import com.google.common.io.Files;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static org.jclouds.compute.config.ComputeServiceProperties.POLL_INITIAL_PERIOD;
import static org.jclouds.compute.config.ComputeServiceProperties.POLL_MAX_PERIOD;
import static org.jclouds.examples.google.computeengine.Constants.*;

/**
 * This example creates a Debian Wheezy server on a f1-micro instance on the
 * Google Compute Engine.
 */
public class CreateServer implements Closeable {
   private final ComputeService computeService;

   /**
    * To get a service account and its private key see [TODO: write some
    * documentation on the website and put a link to it]
    * 
    * The first argument (args[0]) must be your service account email address
    * The second argument (args[1]) must a path to your service account
    *     private key PEM file (without a password).
    */
   public static void main(String[] args) throws IOException {
      String key = Files.toString(new File(args[1]), Charset.defaultCharset());
	   
      CreateServer createServer = new CreateServer(args[0], key);

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

   public CreateServer(String serviceAccountEmailAddress, String serviceAccountKey) {
      // These properties control how often jclouds polls for a status update
      Properties overrides = new Properties();
      overrides.setProperty(POLL_INITIAL_PERIOD, POLL_PERIOD_TWENTY_SECONDS);
      overrides.setProperty(POLL_MAX_PERIOD, POLL_PERIOD_TWENTY_SECONDS);

      ComputeServiceContext context = ContextBuilder.newBuilder(PROVIDER)
            .credentials(serviceAccountEmailAddress, serviceAccountKey)
            .overrides(overrides)
            .buildView(ComputeServiceContext.class);
      computeService = context.getComputeService();
   }

   /**
    * Create a server based on a Template. This method uses Template.hardwareId() and Template.imageId() to
    * also demonstrate iterating through Hardware and Images. Alternatively you do the same without iterating
    * using the following Template.
    * 
    * Template template = computeService.templateBuilder()
    *     .locationId(getLocationId())
    *     .osFamily(OsFamily.CENTOS)
    *     .osVersionMatches("6")
    *     .minRam(28*1024)
    *     .build();
    */
   private void createServer() throws RunNodesException, TimeoutException {
      System.out.format("Create Server%n");

      // TODO: make fromHardware(...) and fromImage(...) work as well. Currently,
      // fromHardware chooses a deprecated platform and the call fails, while using
      // hardwareId() and fromImage() causes no image to be found.
      Template template = computeService.templateBuilder()
            .locationId(ZONE)
            .hardwareId(getHardware().getId())
            .imageId(getImage().getId())
            .build();
      
      // This method will continue to poll for the server status and won't
      // return until this server is ACTIVE.
      // TODO: does GCE also log what's happening during the polling, like for
      // Rackspace? If so, add an example for that.
      Set<? extends NodeMetadata> nodes = computeService.createNodesInGroup(NAME, 1, template);

      NodeMetadata nodeMetadata = nodes.iterator().next();
      String publicAddress = nodeMetadata.getPublicAddresses().iterator().next();

      System.out.format("  %s%n", nodeMetadata);
      System.out.format("  Instance %s started with IP %s%n", nodeMetadata.getName(), publicAddress);
   }

   /**
    * This method uses the generic ComputeService.listHardwareProfiles() to find the hardware profile.
    * 
    * @return The Hardware for a f1-micro instance.
    */
   private Hardware getHardware() {
      System.out.format("  Hardware Profiles%n");

      Set<? extends Hardware> profiles = computeService.listHardwareProfiles();
      Hardware result = null;

      for (Hardware profile : profiles) {
         System.out.format("    %s%n", profile);
         if (profile.getId().equals(ZONE + "/f1-micro")) {
            result = profile;
         }
      }

      if (result == null) {
         System.err.println("f1-micro flavor not found. Using first flavor found:");
         result = profiles.iterator().next();
         System.err.format("-> %s%n", result);
      }
      return result;
   }

   /**
    * This method uses the generic ComputeService.listImages() to find the image.
    * 
    * @return A Debian Wheezy Image 
    */
   private Image getImage() {
      System.out.format("  Images%n");

      Set<? extends Image> images = computeService.listImages();
      Image result = null;

      for (Image image : images) {
         System.out.format("    %s%n", image);
         if (image.getOperatingSystem().getVersion().equals("debian.7.wheezy")) {
            result = image;
         }
      }

      if (result == null) {
         System.err.println("Image with Debian Wheezy operating system not found. Using first image found:");
         result = images.iterator().next();
         System.err.format("-> %s%n", result);
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
