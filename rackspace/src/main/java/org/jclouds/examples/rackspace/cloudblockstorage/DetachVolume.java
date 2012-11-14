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

import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.config.ComputeServiceProperties;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.CinderApiMetadata;
import org.jclouds.openstack.cinder.v1.CinderAsyncApi;
import org.jclouds.openstack.cinder.v1.domain.Volume;
import org.jclouds.openstack.cinder.v1.features.VolumeApi;
import org.jclouds.openstack.cinder.v1.predicates.VolumePredicates;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.VolumeAttachment;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.rest.RestContext;

import com.google.common.collect.FluentIterable;

/**
 * This example detaches the volume created in the CreateVolumeAndAttach example.
 * 
 * @author Everett Toews
 */
public class DetachVolume {
   private static final String NAME = "jclouds-example";
   private static final String ZONE = "DFW";

   private ComputeService compute;
   private RestContext<NovaApi, NovaAsyncApi> nova;
   private ServerApi serverApi;
   private VolumeAttachmentApi volumeAttachmentApi;

   private RestContext<CinderApi, CinderAsyncApi> cinder;
   private VolumeApi volumeApi;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username The second argument
    * (args[1]) must be your API key
    */
   public static void main(String[] args) {
      DetachVolume detachVolume = new DetachVolume();

      try {
         detachVolume.init(args);
         VolumeAttachment volumeAttachment = detachVolume.getVolumeAttachment();
         detachVolume.detachVolume(volumeAttachment);
      } 
      catch (Exception e) {
         e.printStackTrace();
      } 
      finally {
         detachVolume.close();
      }
   }

   private void init(String[] args) {
      // The provider configures jclouds to use the Rackspace open cloud (US)
      // to use the Rackspace open cloud (UK) set the provider to "rackspace-cloudservers-uk"
      String provider = "rackspace-cloudservers-us";

      String username = args[0];
      String apiKey = args[1];

      // These properties control how often jclouds polls for a status udpate
      Properties overrides = new Properties();
      overrides.setProperty(ComputeServiceProperties.POLL_INITIAL_PERIOD, "20000");
      overrides.setProperty(ComputeServiceProperties.POLL_MAX_PERIOD, "20000");

      ComputeServiceContext context = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .overrides(overrides)
            .buildView(ComputeServiceContext.class);
      compute = context.getComputeService();
      nova = context.unwrap();
      volumeAttachmentApi = nova.getApi().getVolumeAttachmentExtensionForZone(ZONE).get();
      serverApi = nova.getApi().getServerApiForZone(ZONE);

      provider = "rackspace-cloudblockstorage-us";

      cinder = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .build(CinderApiMetadata.CONTEXT_TOKEN);
      volumeApi = cinder.getApi().getVolumeApiForZone(ZONE);
   }

   /**
    * @return Server The Server created in the CreateVolumeAndAttach example
    */
   private VolumeAttachment getVolumeAttachment() {
      FluentIterable<? extends Server> servers = serverApi.listInDetail().concat();

      for (Server server : servers) {
         if (server.getName().startsWith(NAME)) {
            FluentIterable<? extends VolumeAttachment> attachments = volumeAttachmentApi.listAttachmentsOnServer(server.getId());
            
            return attachments.iterator().next();
         }
      }

      throw new RuntimeException(NAME + " not found. Run the CreateVolumeAndAttach example first.");
   }

   private void detachVolume(VolumeAttachment volumeAttachment) throws TimeoutException {
      System.out.println("Detach Volume");

      boolean result = volumeAttachmentApi.detachVolumeFromServer(volumeAttachment.getVolumeId(), volumeAttachment.getServerId());

      // Wait for the volume to become Attached (aka In Use) before moving on
      // If you want to know what's happening during the polling, enable
      // logging. See /jclouds-exmaple/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      if (!VolumePredicates.awaitAvailable(volumeApi).apply(Volume.forId(volumeAttachment.getVolumeId()))) {
         throw new TimeoutException("Timeout on volume: " + volumeAttachment.getVolumeId());
      }

      System.out.println("  " + result);
   }

   /**
    * Always close your service when you're done with it.
    */
   private void close() {
      if (compute != null) {
         compute.getContext().close();
      }

      if (cinder != null) {
         cinder.close();
      }
   }
}
