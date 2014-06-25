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
package org.jclouds.examples.rackspace.cloudblockstorage;

import static org.jclouds.compute.config.ComputeServiceProperties.POLL_INITIAL_PERIOD;
import static org.jclouds.compute.config.ComputeServiceProperties.POLL_MAX_PERIOD;
import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.DEVICE;
import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.NAME;
import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.PASSWORD;
import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.POLL_PERIOD_TWENTY_SECONDS;
import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.ZONE;
import static org.jclouds.scriptbuilder.domain.Statements.exec;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.domain.Volume;
import org.jclouds.openstack.cinder.v1.features.VolumeApi;
import org.jclouds.openstack.cinder.v1.options.CreateVolumeOptions;
import org.jclouds.openstack.cinder.v1.predicates.VolumePredicates;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.domain.VolumeAttachment;
import org.jclouds.openstack.nova.v2_0.domain.zonescoped.ZoneAndId;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.rest.RestContext;
import org.jclouds.scriptbuilder.ScriptBuilder;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;

/**
 * This example creates a volume, attaches it to a server, putting a filesystem on it, and mounts it for use.
 */
public class CreateVolumeAndAttach implements Closeable {
   private final ComputeService computeService;
   private final RestContext<NovaApi, NovaAsyncApi> nova;
   private final VolumeAttachmentApi volumeAttachmentApi;

   private final CinderApi cinderApi;
   private final VolumeApi volumeApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      CreateVolumeAndAttach createVolumeAndAttach = new CreateVolumeAndAttach(args[0], args[1]);

      try {
         NodeMetadata node = createVolumeAndAttach.createServer();
         Volume volume = createVolumeAndAttach.createVolume();
         createVolumeAndAttach.attachVolume(volume, node);
         createVolumeAndAttach.mountVolume(node);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createVolumeAndAttach.close();
      }
   }

   public CreateVolumeAndAttach(String username, String apiKey) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the system property or default value to "rackspace-cloudservers-uk"
      String provider = System.getProperty("provider.cs", "rackspace-cloudservers-us");

      // These properties control how often jclouds polls for a status udpate
      Properties overrides = new Properties();
      overrides.setProperty(POLL_INITIAL_PERIOD, POLL_PERIOD_TWENTY_SECONDS);
      overrides.setProperty(POLL_MAX_PERIOD, POLL_PERIOD_TWENTY_SECONDS);

      Iterable<Module> modules = ImmutableSet.<Module> of(new SshjSshClientModule());

      ComputeServiceContext context = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .modules(modules)
            .overrides(overrides)
            .buildView(ComputeServiceContext.class);
      computeService = context.getComputeService();
      nova = context.unwrap();
      volumeAttachmentApi = nova.getApi().getVolumeAttachmentExtensionForZone(ZONE).get();

      cinderApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CinderApi.class);
      volumeApi = cinderApi.getVolumeApiForZone(ZONE);
   }

   private NodeMetadata createServer() throws RunNodesException, TimeoutException {
      System.out.format("Create Server%n");

      ZoneAndId zoneAndId = ZoneAndId.fromZoneAndId(ZONE, "performance1-1");
      Template template = computeService.templateBuilder()
            .locationId(ZONE)
            .osDescriptionMatches(".*Ubuntu 12.04.*")
            .hardwareId(zoneAndId.slashEncode())
            .build();

      Set<? extends NodeMetadata> nodes = computeService.createNodesInGroup(NAME, 1, template);
      NodeMetadata nodeMetadata = nodes.iterator().next();
      String publicAddress = nodeMetadata.getPublicAddresses().iterator().next();

      // We set the password to something we know so we can login in the DetachVolume example
      nova.getApi().getServerApiForZone(ZONE)
            .changeAdminPass(nodeMetadata.getProviderId(), PASSWORD);

      System.out.format("  %s%n", nodeMetadata);
      System.out.format("  Login: ssh %s@%s%n", nodeMetadata.getCredentials().getUser(), publicAddress);
      System.out.format("  Password: %s%n", PASSWORD);

      return nodeMetadata;
   }

   private Volume createVolume() throws TimeoutException {
      CreateVolumeOptions options = CreateVolumeOptions.Builder
            .name(NAME)
            .volumeType("SSD")
            .metadata(ImmutableMap.<String, String> of("key1", "value1"));

      System.out.format("Create Volume%n");

      // 100 GB is the minimum volume size on the Rackspace Cloud
      Volume volume = volumeApi.create(100, options);

      // Wait for the volume to become Available before moving on
      // If you want to know what's happening during the polling, enable logging. See
      // /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      if (!VolumePredicates.awaitAvailable(volumeApi).apply(volume)) {
         throw new TimeoutException("Timeout on volume: " + volume);
      }

      System.out.format("  %s%n", volume);

      return volume;
   }

   private void attachVolume(Volume volume, NodeMetadata node) throws TimeoutException {
      System.out.format("Create Volume Attachment%n");

      // Note the use of NodeMetadata.getProviderId()
      // This is necessary as NodeMetadata.getId() will return a Location/Id combination
      VolumeAttachment volumeAttachment = volumeAttachmentApi
            .attachVolumeToServerAsDevice(volume.getId(), node.getProviderId(), DEVICE);

      // Wait for the volume to become Attached (aka In Use) before moving on
      if (!VolumePredicates.awaitInUse(volumeApi).apply(volume)) {
         throw new TimeoutException("Timeout on volume: " + volume);
      }

      System.out.format("  %s%n", volumeAttachment);
   }

   private void mountVolume(NodeMetadata node) {
      System.out.format("Mount Volume and Create Filesystem%n");

      String script = new ScriptBuilder()
            .addStatement(exec("mkfs -t ext4 /dev/xvdd"))
            .addStatement(exec("mount /dev/xvdd /mnt"))
            .render(OsFamily.UNIX);

      RunScriptOptions options = RunScriptOptions.Builder
            .blockOnComplete(true)
            .overrideLoginPassword(PASSWORD);

      ExecResponse response = computeService.runScriptOnNode(node.getId(), script, options);

      if (response.getExitStatus() == 0) {
         System.out.format("  Exit Status: %s%n", response.getExitStatus());
      }
      else {
         System.out.format("  Error: %s%n", response.getOutput());
      }
   }

   /**
    * Always close your service when you're done with it.
    *
    * Note that closing quietly like this is not necessary in Java 7.
    * You would use try-with-resources in the main method instead.
    */
   public void close() throws IOException {
      Closeables.close(cinderApi, true);
      Closeables.close(computeService.getContext(), true);
   }
}
