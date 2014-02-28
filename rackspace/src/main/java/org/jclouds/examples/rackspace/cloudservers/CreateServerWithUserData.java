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

import static org.jclouds.compute.config.ComputeServiceProperties.POLL_INITIAL_PERIOD;
import static org.jclouds.compute.config.ComputeServiceProperties.POLL_MAX_PERIOD;
import static org.jclouds.examples.rackspace.cloudservers.Constants.NAME;
import static org.jclouds.examples.rackspace.cloudservers.Constants.POLL_PERIOD_TWENTY_SECONDS;
import static org.jclouds.examples.rackspace.cloudservers.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudservers.Constants.ZONE;
import static org.jclouds.scriptbuilder.domain.Statements.exec;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v2_0.domain.zonescoped.ZoneAndId;
import org.jclouds.rest.RestContext;
import org.jclouds.scriptbuilder.ScriptBuilder;
import org.jclouds.scriptbuilder.domain.OsFamily;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.Closeables;
import com.google.inject.Module;

import org.apache.commons.codec.binary.Base64;

/**
 * This demonstrates how to apply user data and drive config to run cloud-init on the rackspace cloud (or nova-compatible clouds).
 * The user data is a cloud-init config file, which is base-64 encoded by jclouds.
 * The drive config needs to be set to true so that the supported images (not all images support cloud-init)
 * can read the cloud-init config file.
 * To check cloud-init status, see the /var/log/cloud-init.log logfile.
 */
public class CreateServerWithUserData implements Closeable {
   private final ComputeService computeService;
   private final RestContext<NovaApi, NovaAsyncApi> novaContext;

   private final File UserDataFile = new File(NAME + ".pem");

   /**
    * To get a username and API key see
    * http://jclouds.apache.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      System.out.format("CreateServerWithUserData%n");

      CreateServerWithUserData createServerWithUserData = new CreateServerWithUserData(args[0], args[1]);

      try {
         createServerWithUserData.createServer();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createServerWithUserData.close();
      }
   }

   public CreateServerWithUserData(String username, String apiKey) {

      // These properties control how often jclouds polls for a status update
      Properties overrides = new Properties();
      overrides.setProperty(POLL_INITIAL_PERIOD, POLL_PERIOD_TWENTY_SECONDS);
      overrides.setProperty(POLL_MAX_PERIOD, POLL_PERIOD_TWENTY_SECONDS);
      
      // This module is responsible for enabling logging
      Iterable<Module> modules = ImmutableSet.<Module> of(new SLF4JLoggingModule());
      ComputeServiceContext context = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .overrides(overrides)
            .modules(modules)
            .buildView(ComputeServiceContext.class);

      computeService = context.getComputeService();
      novaContext = context.unwrap();
   }

   /**
    * Create a server with cloud-init.
    */
   private NodeMetadata createServer() throws RunNodesException, TimeoutException {
      System.out.format("  Create Server%n");      

      String userData = 
            "#cloud-config\r\n" + 
                  "\r\n" + 
                  "# boot commands\r\n" + 
                  "# default: none\r\n" + 
                  "# this is very similar to runcmd, but commands run very early\r\n" + 
                  "# in the boot process, only slightly after a 'boothook' would run.\r\n" + 
                  "# bootcmd should really only be used for things that could not be\r\n" + 
                  "# done later in the boot process.  bootcmd is very much like\r\n" + 
                  "# boothook, but possibly with more friendly.\r\n" + 
                  "#  * bootcmd will run on every boot\r\n" + 
                  "#  * the INSTANCE_ID variable will be set to the current instance id.\r\n" + 
                  "#  * you can use 'cloud-init-boot-per' command to help only run once\r\n" + 
                  "bootcmd:\r\n" + 
                  " - echo 192.168.1.130 us.archive.ubuntu.com > /etc/hosts\r\n" + 
                  " - echo 1.1.1.1 something.com > /etc/hosts\r\n" + 
                  " - [ cloud-init-per, once, mymkfs, mkfs, /dev/vdb ]\r\n" + 
                  "\r\n" + 
                  "packages:\r\n" + 
                  " - httpd\r\n";

      // The data will be base64 encoded.
      NovaTemplateOptions options = NovaTemplateOptions.Builder.userData(userData.getBytes()).configDrive(true);

      ZoneAndId zoneAndId = ZoneAndId.fromZoneAndId(ZONE, "performance1-1");
      Template template = computeService.templateBuilder()
            .locationId(ZONE)
            .osDescriptionMatches(".*Ubuntu 12.04.*") // Only some images support cloud init!
            .hardwareId(zoneAndId.slashEncode())
            .options(options)
            .build();

      // This method will continue to poll for the server status and won't return until this server is ACTIVE
      // If you want to know what's happening during the polling, enable logging.
      // @see https://github.com/jclouds/jclouds-examples/blob/master/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      Set<? extends NodeMetadata> nodes = computeService.createNodesInGroup(NAME, 1, template);
      NodeMetadata node = Iterables.getOnlyElement(nodes);

      System.out.format("    %s%n", node);

      return node;
   }   

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(computeService.getContext(), true);
   }
}
