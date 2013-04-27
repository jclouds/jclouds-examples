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
package org.jclouds.examples.rackspace.cloudblockstorage;

import static com.google.common.io.Closeables.closeQuietly;
import static org.jclouds.scriptbuilder.domain.Statements.exec;

import java.io.Closeable;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.config.ComputeServiceProperties;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.CinderApiMetadata;
import org.jclouds.openstack.cinder.v1.CinderAsyncApi;
import org.jclouds.openstack.cinder.v1.domain.Volume;
import org.jclouds.openstack.cinder.v1.features.VolumeApi;
import org.jclouds.openstack.cinder.v1.options.CreateVolumeOptions;
import org.jclouds.openstack.cinder.v1.predicates.VolumePredicates;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.domain.VolumeAttachment;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.rest.RestContext;
import org.jclouds.scriptbuilder.ScriptBuilder;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

/**
 * This example creates a volume, attaches it to a server, putting a filesystem on it, and mounts it for use.
 * 
 * @author Everett Toews
 */
public class CreateVolumeAndAttach implements Closeable {
   private ComputeService compute;
   private RestContext<NovaApi, NovaAsyncApi> nova;
   private VolumeAttachmentApi volumeAttachmentApi;

   private RestContext<CinderApi, CinderAsyncApi> cinder;
   private VolumeApi volumeApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      CreateVolumeAndAttach createVolumeAndAttach = new CreateVolumeAndAttach();

      try {
         createVolumeAndAttach.init(args);
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

      Iterable<Module> modules = ImmutableSet.<Module> of(new SshjSshClientModule());

      ComputeServiceContext context = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .modules(modules)
            .overrides(overrides)
            .buildView(ComputeServiceContext.class);
      compute = context.getComputeService();
      nova = context.unwrap();
      volumeAttachmentApi = nova.getApi().getVolumeAttachmentExtensionForZone(Constants.ZONE).get();

      provider = "rackspace-cloudblockstorage-us";

      cinder = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .modules(modules)
            .build(CinderApiMetadata.CONTEXT_TOKEN);
      volumeApi = cinder.getApi().getVolumeApiForZone(Constants.ZONE);
   }

   private NodeMetadata createServer() throws RunNodesException, TimeoutException {
      Template template = compute.templateBuilder()
            .locationId(Constants.ZONE)
            .osDescriptionMatches(".*CentOS 6.2.*")
            .minRam(512).build();

      System.out.println("Create Server");

      Set<? extends NodeMetadata> nodes = compute.createNodesInGroup(Constants.NAME, 1, template);
      NodeMetadata nodeMetadata = nodes.iterator().next();
      String publicAddress = nodeMetadata.getPublicAddresses().iterator().next();

      // We set the password to something we know so we can login in the DetachVolume example
      nova.getApi().getServerApiForZone(Constants.ZONE)
            .changeAdminPass(nodeMetadata.getProviderId(), Constants.PASSWORD);

      System.out.println("  " + nodeMetadata);
      System.out.println("  Login: ssh " + nodeMetadata.getCredentials().getUser() + "@" + publicAddress);
      System.out.println("  Password: " + Constants.PASSWORD);

      return nodeMetadata;
   }

   private Volume createVolume() throws TimeoutException {
      CreateVolumeOptions options = CreateVolumeOptions.Builder
            .name(Constants.NAME)
            .metadata(ImmutableMap.<String, String> of("key1", "value1"));

      System.out.println("Create Volume");

      // 100 GB is the minimum volume size on the Rackspace Cloud
      Volume volume = volumeApi.create(100, options);

      // Wait for the volume to become Available before moving on
      // If you want to know what's happening during the polling, enable logging. See
      // /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      if (!VolumePredicates.awaitAvailable(volumeApi).apply(volume)) {
         throw new TimeoutException("Timeout on volume: " + volume);
      }

      System.out.println("  " + volume);

      return volume;
   }

   private void attachVolume(Volume volume, NodeMetadata node) throws TimeoutException {
      System.out.println("Create Volume Attachment");

      // Note the use of NodeMetadata.getProviderId()
      // This is necessary as NodeMetadata.getId() will return a Location/Id combination
      VolumeAttachment volumeAttachment = volumeAttachmentApi
            .attachVolumeToServerAsDevice(volume.getId(), node.getProviderId(), Constants.DEVICE);

      // Wait for the volume to become Attached (aka In Use) before moving on
      if (!VolumePredicates.awaitInUse(volumeApi).apply(volume)) {
         throw new TimeoutException("Timeout on volume: " + volume);
      }

      System.out.println("  " + volumeAttachment);
   }

   private void mountVolume(NodeMetadata node) {
      System.out.println("Mount Volume and Create Filesystem");

      String script = new ScriptBuilder()
            .addStatement(exec("mkfs -t ext4 /dev/xvdd"))
            .addStatement(exec("mount /dev/xvdd /mnt"))
            .render(OsFamily.UNIX);

      RunScriptOptions options = RunScriptOptions.Builder
            .blockOnComplete(true)
            .overrideLoginPassword(Constants.PASSWORD);

      ExecResponse response = compute.runScriptOnNode(node.getId(), script, options);

      if (response.getExitStatus() == 0) {
         System.out.println("  Exit Status: " + response.getExitStatus());
      }
      else {
         System.out.println("  Error: " + response.getOutput());
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(cinder);
      closeQuietly(compute.getContext());
   }
}
