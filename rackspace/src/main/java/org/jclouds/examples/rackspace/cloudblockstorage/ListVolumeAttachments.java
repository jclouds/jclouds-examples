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

import java.io.Closeable;

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

import com.google.common.collect.FluentIterable;

/**
 * This example lists the volume attachments of a server.
 * 
 * @author Everett Toews
 */
public class ListVolumeAttachments implements Closeable {
   private ComputeService compute;
   private RestContext<NovaApi, NovaAsyncApi> nova;
   private ServerApi serverApi;
   private VolumeAttachmentApi volumeAttachmentApi;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username The second argument
    * (args[1]) must be your API key
    */
   public static void main(String[] args) {
      ListVolumeAttachments listVolumeAttachments = new ListVolumeAttachments();

      try {
         listVolumeAttachments.init(args);
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

   private void init(String[] args) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the provider to "rackspace-cloudservers-uk"
      String provider = "rackspace-cloudservers-us";

      String username = args[0];
      String apiKey = args[1];

      ComputeServiceContext context = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .buildView(ComputeServiceContext.class);
      compute = context.getComputeService();
      nova = context.unwrap();
      volumeAttachmentApi = nova.getApi().getVolumeAttachmentExtensionForZone(Constants.ZONE).get();
      serverApi = nova.getApi().getServerApiForZone(Constants.ZONE);
   }

   /**
    * @return Server The Server created in the CreateVolumeAndAttach example
    */
   private Server getServer() {
      FluentIterable<? extends Server> servers = serverApi.listInDetail().concat();

      for (Server server: servers) {
         if (server.getName().startsWith(Constants.NAME)) {
            return server;
         }
      }

      throw new RuntimeException(Constants.NAME + " not found. Run the CreateVolumeAndAttach example first.");
   }

   private void listVolumeAttachments(Server server) {
      System.out.println("List Volume Attachments");

      for (VolumeAttachment volumeAttachment: volumeAttachmentApi.listAttachmentsOnServer(server.getId())) {
         System.out.println("  " + volumeAttachment);
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(compute.getContext());
   }
}
