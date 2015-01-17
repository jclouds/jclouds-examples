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
package org.jclouds.examples.rackspace;

import com.google.common.collect.Iterables;
import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.domain.Snapshot;
import org.jclouds.openstack.cinder.v1.domain.Volume;
import org.jclouds.openstack.cinder.v1.features.SnapshotApi;
import org.jclouds.openstack.cinder.v1.features.VolumeApi;
import org.jclouds.openstack.marconi.v1.MarconiApi;
import org.jclouds.openstack.marconi.v1.domain.Queue;
import org.jclouds.openstack.marconi.v1.features.QueueApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.KeyPair;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.VolumeAttachment;
import org.jclouds.openstack.nova.v2_0.extensions.KeyPairApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.swift.v1.domain.Container;
import org.jclouds.openstack.swift.v1.domain.ObjectList;
import org.jclouds.openstack.swift.v1.domain.SwiftObject;
import org.jclouds.openstack.swift.v1.features.ContainerApi;
import org.jclouds.openstack.swift.v1.features.ObjectApi;
import org.jclouds.openstack.trove.v1.TroveApi;
import org.jclouds.openstack.trove.v1.domain.Instance;
import org.jclouds.openstack.trove.v1.domain.User;
import org.jclouds.openstack.trove.v1.features.DatabaseApi;
import org.jclouds.openstack.trove.v1.features.InstanceApi;
import org.jclouds.openstack.trove.v1.features.UserApi;
import org.jclouds.rackspace.autoscale.v1.AutoscaleApi;
import org.jclouds.rackspace.autoscale.v1.domain.GroupState;
import org.jclouds.rackspace.autoscale.v1.features.GroupApi;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;
import org.jclouds.rackspace.cloudfiles.v1.CloudFilesApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.CloudLoadBalancersApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.LoadBalancer;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.LoadBalancerApi;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.jclouds.examples.rackspace.clouddns.Constants.GET_DOMAIN_ID;
import static org.jclouds.examples.rackspace.clouddns.Constants.IS_DOMAIN;
import static org.jclouds.rackspace.clouddns.v1.predicates.JobPredicates.awaitComplete;

/**
 * Indiscriminately delete all resources in all regions of an account.
 */
public class DeleteAll {
   private final String username;
   private final String apiKey;

    /**
    * To get a username and API key see http://jclouds.apache.org/guides/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key or password
    */
   public static void main(String[] args) {
      DeleteAll deleteAll = new DeleteAll(args);

      try {
         deleteAll.deleteCloudFiles();
         deleteAll.deleteCloudServers();
         deleteAll.deleteCloudBlockStorage();
         deleteAll.deleteCloudDatabases();
         deleteAll.deleteCloudDNS();
         deleteAll.deleteLoadBalancers();
         deleteAll.deleteQueues();
         deleteAll.deleteAutoscale();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public DeleteAll(String[] args) {
      username = args[0];
      apiKey = args[1];
   }

   private void deleteCloudFiles() throws IOException {
      CloudFilesApi cloudFilesApi = ContextBuilder.newBuilder(System.getProperty("provider.cf", "rackspace-cloudfiles-us"))
            .credentials(username, apiKey)
            .buildApi(CloudFilesApi.class);

      for (String region : cloudFilesApi.getConfiguredRegions()) {
         try {
            System.out.format("Delete Containers of Objects in %s%n", region);
            ContainerApi containerApi = cloudFilesApi.getContainerApi(region);

            for (Container container : containerApi.list()) {
               System.out.format("  %s%n", container.getName());

               ObjectApi objectApi = cloudFilesApi.getObjectApi(region, container.getName());
               ObjectList objects = objectApi.list();

               for (SwiftObject object : objects) {
                  System.out.format("    %s%n", object.getName());
                  objectApi.delete(object.getName());
               }

               cloudFilesApi.getContainerApi(region).deleteIfEmpty(container.getName());
            }
         } catch (IllegalStateException e) {
            e.printStackTrace();
         }
      }

      Closeables.close(cloudFilesApi, true);
   }

   private void deleteCloudBlockStorage() throws IOException {
      CinderApi cinderApi = ContextBuilder.newBuilder(System.getProperty("provider.cbs", "rackspace-cloudblockstorage-us"))
            .credentials(username, apiKey)
            .buildApi(CinderApi.class);

      for (String zone : cinderApi.getConfiguredZones()) {
         try {
            System.out.format("Delete Snapshots in %s%n", zone);
            SnapshotApi snapshotApi = cinderApi.getSnapshotApiForZone(zone);

            for (Snapshot snapshot : snapshotApi.list()) {
               System.out.format("  %s%n", snapshot.getName());
               snapshotApi.delete(snapshot.getId());
            }

            System.out.format("Delete Volumes in %s%n", zone);
            VolumeApi volumeApi = cinderApi.getVolumeApiForZone(zone);

            for (Volume volume : volumeApi.list()) {
               System.out.format("  %s%n", volume.getName());
               volumeApi.delete(volume.getId());
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      Closeables.close(cinderApi, true);
   }

   private void deleteCloudServers() throws IOException {
      NovaApi novaApi = ContextBuilder.newBuilder(System.getProperty("provider.cs", "rackspace-cloudservers-us"))
            .credentials(username, apiKey)
            .buildApi(NovaApi.class);

      for (String zone : novaApi.getConfiguredZones()) {
         try {
            System.out.format("Delete Key Pairs in %s%n", zone);
            KeyPairApi keyPairApi = novaApi.getKeyPairExtensionForZone(zone).get();

            for (KeyPair keyPair : keyPairApi.list()) {
               System.out.format("  %s%n", keyPair.getName());
               keyPairApi.delete(keyPair.getName());
            }

            System.out.format("Delete Servers in %s%n", zone);
            VolumeAttachmentApi volumeAttachmentApi = novaApi.getVolumeAttachmentExtensionForZone(zone).get();
            ServerApi serverApi = novaApi.getServerApiForZone(zone);

            for (Server server : serverApi.listInDetail().concat().toList()) {
               for (VolumeAttachment volumeAttachment : volumeAttachmentApi.listAttachmentsOnServer(server.getId())) {
                  volumeAttachmentApi.detachVolumeFromServer(volumeAttachment.getId(), server.getId());
               }

               System.out.format("  %s%n", server.getName());
               serverApi.delete(server.getId());
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      Closeables.close(novaApi, true);
   }

   private void deleteCloudDatabases() throws IOException {
      TroveApi troveApi = ContextBuilder.newBuilder(System.getProperty("provider.cdb", "rackspace-clouddatabases-us"))
            .credentials(username, apiKey)
            .buildApi(TroveApi.class);

      for (String zone : troveApi.getConfiguredZones()) {
         try {
            System.out.format("Delete Database Instances of DBs and Users in %s%n", zone);
            InstanceApi instanceApi = troveApi.getInstanceApiForZone(zone);

            for (Instance instance : instanceApi.list()) {
               System.out.format("  %s%n", instance.getName());
               DatabaseApi databaseApi = troveApi.getDatabaseApiForZoneAndInstance(zone, instance.getId());

               for (String database : databaseApi.list()) {
                  System.out.format("    %s%n", database);
                  databaseApi.delete(database);
               }

               UserApi userApi = troveApi.getUserApiForZoneAndInstance(zone, instance.getId());

               for (User user : userApi.list()) {
                  System.out.format("    %s%n", user.getName());
                  userApi.delete(user.getName());
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      Closeables.close(troveApi, true);
   }

   private void deleteCloudDNS() throws IOException, TimeoutException {
      System.out.format("Delete DNS Domains%n");

      CloudDNSApi cloudDNSApi = ContextBuilder.newBuilder(System.getProperty("provider.cdns", "rackspace-clouddns-us"))
            .credentials(username, apiKey)
            .buildApi(CloudDNSApi.class);

      try {
         Set<Domain> allDomains = cloudDNSApi.getDomainApi().list().concat().toSet();
         Iterable<Domain> topLevelDomains = Iterables.filter(allDomains, IS_DOMAIN);
         Iterable<Integer> topLevelDomainIds = Iterables.transform(topLevelDomains, GET_DOMAIN_ID);

         if (!allDomains.isEmpty()) {
            awaitComplete(cloudDNSApi, cloudDNSApi.getDomainApi().delete(topLevelDomainIds, true));
            System.out.format("  Deleted %s domains%n", Iterables.size(topLevelDomainIds));
         }
      } catch (TimeoutException e) {
         e.printStackTrace();
      }

      Closeables.close(cloudDNSApi, true);
   }

   private void deleteLoadBalancers() throws IOException {
      CloudLoadBalancersApi clbApi = ContextBuilder.newBuilder(System.getProperty("provider.clb", "rackspace-cloudloadbalancers-us"))
            .credentials(username, apiKey)
            .buildApi(CloudLoadBalancersApi.class);

      for (String zone : clbApi.getConfiguredZones()) {
         try {
            System.out.format("Delete Load Balancers in %s%n", zone);
            LoadBalancerApi lbApi = clbApi.getLoadBalancerApiForZone(zone);

            for (LoadBalancer loadBalancer : lbApi.list().concat()) {
               System.out.format("  %s%n", loadBalancer.getName());
               lbApi.delete(loadBalancer.getId());
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      Closeables.close(clbApi, true);
   }

   private void deleteQueues() throws IOException {
      MarconiApi marconiApi = ContextBuilder.newBuilder(System.getProperty("provider.cq", "rackspace-cloudqueues-us"))
            .credentials(username, apiKey)
            .buildApi(MarconiApi.class);
      UUID uuid = UUID.randomUUID(); // any UUID can be used to list all queues

      for (String zone : marconiApi.getConfiguredZones()) {
         try {
            System.out.format("Delete Queues in %s%n", zone);
            QueueApi queueApi = marconiApi.getQueueApiForZoneAndClient(zone, uuid);

            for (Queue queue : queueApi.list(false).concat()) {
               System.out.format("  %s%n", queue.getName());
               queueApi.delete(queue.getName());
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      Closeables.close(marconiApi, true);
   }

   private void deleteAutoscale() throws IOException {
      AutoscaleApi autoscaleApi = ContextBuilder.newBuilder(System.getProperty("provider.cq", "rackspace-autoscale-us"))
            .credentials(username, apiKey)
            .buildApi(AutoscaleApi.class);

      for (String zone : autoscaleApi.getConfiguredZones()) {
         try {
            System.out.format("Delete Autoscale Groups in %s%n", zone);
            GroupApi groupApi = autoscaleApi.getGroupApiForZone(zone);

            for (GroupState groupState : groupApi.listGroupStates()) {
               System.out.format("  %s%n", groupState.getId());
               groupApi.delete(groupState.getId());
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      Closeables.close(autoscaleApi, true);
   }
}
