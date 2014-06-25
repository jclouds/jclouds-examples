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

import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.NAME;
import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.PASSWORD;
import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.ROOT;
import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.ZONE;
import static org.jclouds.scriptbuilder.domain.Statements.exec;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.domain.Volume;
import org.jclouds.openstack.cinder.v1.features.VolumeApi;
import org.jclouds.openstack.cinder.v1.predicates.VolumePredicates;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.VolumeAttachment;
import org.jclouds.openstack.nova.v2_0.domain.zonescoped.ZoneAndId;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.rest.RestContext;
import org.jclouds.scriptbuilder.ScriptBuilder;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;

/**
 * This example detaches the volume created in the CreateVolumeAndAttach example.
 */
public class DetachVolume implements Closeable {
   private final ComputeService computeService;
   private final RestContext<NovaApi, NovaAsyncApi> nova;
   private final ServerApi serverApi;
   private final VolumeAttachmentApi volumeAttachmentApi;

   private final CinderApi cinderApi;
   private final VolumeApi volumeApi;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      DetachVolume detachVolume = new DetachVolume(args[0], args[1]);

      try {
         VolumeAttachment volumeAttachment = detachVolume.getVolumeAttachment();
         detachVolume.unmountVolume(volumeAttachment);
         detachVolume.detachVolume(volumeAttachment);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         detachVolume.close();
      }
   }

   public DetachVolume(String username, String apiKey) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the system property or default value to "rackspace-cloudservers-uk"
      String provider = System.getProperty("provider.cs", "rackspace-cloudservers-us");

      Iterable<Module> modules = ImmutableSet.<Module> of(new SshjSshClientModule());

      ComputeServiceContext context = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .modules(modules)
            .buildView(ComputeServiceContext.class);
      computeService = context.getComputeService();
      nova = context.unwrap();
      serverApi = nova.getApi().getServerApiForZone(ZONE);
      volumeAttachmentApi = nova.getApi().getVolumeAttachmentExtensionForZone(ZONE).get();

      cinderApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CinderApi.class);
      volumeApi = cinderApi.getVolumeApiForZone(ZONE);
   }

   /**
    * @return Server The Server created in the CreateVolumeAndAttach example
    */
   private VolumeAttachment getVolumeAttachment() {
      FluentIterable<? extends Server> servers = serverApi.listInDetail().concat();

      for (Server server: servers) {
         if (server.getName().startsWith(NAME)) {
            FluentIterable<? extends VolumeAttachment> attachments = volumeAttachmentApi
                  .listAttachmentsOnServer(server.getId());

            return attachments.iterator().next();
         }
      }

      throw new RuntimeException(NAME + " not found. Run the CreateVolumeAndAttach example first.");
   }

   /**
    * Make sure you've unmounted the volume first. Failure to do so could result in failure or data loss.
    */
   private void unmountVolume(VolumeAttachment volumeAttachment) {
      System.out.format("Unmount Volume%n");

      String script = new ScriptBuilder().addStatement(exec("umount /mnt")).render(OsFamily.UNIX);

      RunScriptOptions options = RunScriptOptions.Builder
            .overrideLoginUser(ROOT)
            .overrideLoginPassword(PASSWORD)
            .blockOnComplete(true);

      ZoneAndId zoneAndId = ZoneAndId.fromZoneAndId(ZONE, volumeAttachment.getServerId());
      ExecResponse response = computeService.runScriptOnNode(zoneAndId.slashEncode(), script, options);

      if (response.getExitStatus() == 0) {
         System.out.format("  Exit Status: %s%n", response.getExitStatus());
      }
      else {
         System.out.format("  Error: %s%n",response.getOutput());
      }
   }

   private void detachVolume(VolumeAttachment volumeAttachment) throws TimeoutException {
      System.out.format("Detach Volume%n");

      boolean result = volumeAttachmentApi
            .detachVolumeFromServer(volumeAttachment.getVolumeId(), volumeAttachment.getServerId());

      // Wait for the volume to become Attached (aka In Use) before moving on
      // If you want to know what's happening during the polling, enable
      // logging. See /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      if (!VolumePredicates.awaitAvailable(volumeApi).apply(Volume.forId(volumeAttachment.getVolumeId()))) {
         throw new TimeoutException("Timeout on volume: " + volumeAttachment.getVolumeId());
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
      Closeables.close(cinderApi, true);
      Closeables.close(computeService.getContext(), true);
   }
}
