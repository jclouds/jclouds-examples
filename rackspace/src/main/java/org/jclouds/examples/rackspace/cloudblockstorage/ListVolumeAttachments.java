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

import com.google.common.collect.FluentIterable;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.VolumeAttachment;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.rest.RestContext;

import java.io.Closeable;

import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.NAME;
import static org.jclouds.examples.rackspace.cloudblockstorage.Constants.ZONE;

/**
 * This example lists the volume attachments of a server.
 * 
 * @author Everett Toews
 */
public class ListVolumeAttachments implements Closeable {
   private final ComputeService computeService;
   private final RestContext<NovaApi, NovaAsyncApi> nova;
   private final ServerApi serverApi;
   private final VolumeAttachmentApi volumeAttachmentApi;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      ListVolumeAttachments listVolumeAttachments = new ListVolumeAttachments(args[0], args[1]);

      try {
         Server server = listVolumeAttachments.getServer();
         listVolumeAttachments.listVolumeAttachments(server);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         listVolumeAttachments.close();
      }
   }

   public ListVolumeAttachments(String username, String apiKey) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the system property or default value to "rackspace-cloudservers-uk"
      String provider = System.getProperty("provider.cs", "rackspace-cloudservers-us");

      ComputeServiceContext context = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .buildView(ComputeServiceContext.class);
      computeService = context.getComputeService();
      nova = context.unwrap();
      serverApi = nova.getApi().getServerApiForZone(ZONE);
      volumeAttachmentApi = nova.getApi().getVolumeAttachmentExtensionForZone(ZONE).get();
   }

   /**
    * @return Server The Server created in the CreateVolumeAndAttach example
    */
   private Server getServer() {
      FluentIterable<? extends Server> servers = serverApi.listInDetail().concat();

      for (Server server: servers) {
         if (server.getName().startsWith(NAME)) {
            return server;
         }
      }

      throw new RuntimeException(NAME + " not found. Run the CreateVolumeAndAttach example first.");
   }

   private void listVolumeAttachments(Server server) {
      System.out.format("List Volume Attachments%n");

      for (VolumeAttachment volumeAttachment: volumeAttachmentApi.listAttachmentsOnServer(server.getId())) {
         System.out.format("  %s%n", volumeAttachment);
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      if (computeService != null) {
         computeService.getContext().close();
      }
   }
}
