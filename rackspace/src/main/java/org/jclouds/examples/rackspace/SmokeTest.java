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
package org.jclouds.examples.rackspace;

import org.jclouds.examples.rackspace.cloudblockstorage.CreateSnapshot;
import org.jclouds.examples.rackspace.cloudblockstorage.CreateVolumeAndAttach;
import org.jclouds.examples.rackspace.cloudblockstorage.DeleteSnapshot;
import org.jclouds.examples.rackspace.cloudblockstorage.DeleteVolume;
import org.jclouds.examples.rackspace.cloudblockstorage.DetachVolume;
import org.jclouds.examples.rackspace.cloudblockstorage.ListSnapshots;
import org.jclouds.examples.rackspace.cloudblockstorage.ListVolumeAttachments;
import org.jclouds.examples.rackspace.cloudblockstorage.ListVolumeTypes;
import org.jclouds.examples.rackspace.cloudblockstorage.ListVolumes;
import org.jclouds.examples.rackspace.clouddns.CRUDReverseDNSRecords;
import org.jclouds.examples.rackspace.clouddns.CreateDomains;
import org.jclouds.examples.rackspace.clouddns.CreateRecords;
import org.jclouds.examples.rackspace.clouddns.DeleteDomains;
import org.jclouds.examples.rackspace.clouddns.DeleteRecords;
import org.jclouds.examples.rackspace.clouddns.ListDomains;
import org.jclouds.examples.rackspace.clouddns.ListRecords;
import org.jclouds.examples.rackspace.clouddns.UpdateDomains;
import org.jclouds.examples.rackspace.clouddns.UpdateRecords;
import org.jclouds.examples.rackspace.cloudfiles.CloudFilesPublish;
import org.jclouds.examples.rackspace.cloudfiles.CreateContainer;
import org.jclouds.examples.rackspace.cloudfiles.DeleteObjectsAndContainer;
import org.jclouds.examples.rackspace.cloudfiles.ListContainers;
import org.jclouds.examples.rackspace.cloudfiles.ListObjects;
import org.jclouds.examples.rackspace.cloudfiles.UploadObjects;
import org.jclouds.examples.rackspace.cloudloadbalancers.AddNodes;
import org.jclouds.examples.rackspace.cloudloadbalancers.CreateLoadBalancerWithExistingServers;
import org.jclouds.examples.rackspace.cloudloadbalancers.CreateLoadBalancerWithNewServers;
import org.jclouds.examples.rackspace.cloudloadbalancers.DeleteLoadBalancers;
import org.jclouds.examples.rackspace.cloudloadbalancers.ListLoadBalancers;
import org.jclouds.examples.rackspace.cloudloadbalancers.RemoveNodes;
import org.jclouds.examples.rackspace.cloudloadbalancers.UpdateLoadBalancers;
import org.jclouds.examples.rackspace.cloudloadbalancers.UpdateNodes;
import org.jclouds.examples.rackspace.cloudservers.CloudServersPublish;
import org.jclouds.examples.rackspace.cloudservers.CreateServer;
import org.jclouds.examples.rackspace.cloudservers.DeleteServer;
import org.jclouds.examples.rackspace.cloudservers.ListServersWithFiltering;
import org.jclouds.examples.rackspace.cloudservers.ServerMetadata;

/**
 * This example smoke tests all of the other examples in these packages.
 * 
 * @author Everett Toews
 */
public class SmokeTest {

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username The second argument
    * (args[1]) must be your API key
    */
   public static void main(String[] args) {
      SmokeTest smokeTest = new SmokeTest();
      smokeTest.smokeTest(args);
   }

   private void smokeTest(String[] args) {
      Authentication.main(args);
      Logging.main(args);

      CloudServersPublish.main(args);
      CreateServer.main(args);
      ListServersWithFiltering.main(args);
      ServerMetadata.main(args);
      DeleteServer.main(args);

      CloudFilesPublish.main(args);
      CreateContainer.main(args);
      ListContainers.main(args);
      UploadObjects.main(args);
      ListObjects.main(args);
      DeleteObjectsAndContainer.main(args);

      CreateVolumeAndAttach.main(args);
      ListVolumes.main(args);
      ListVolumeAttachments.main(args);
      ListVolumeTypes.main(args);
      DetachVolume.main(args);
      CreateSnapshot.main(args);
      ListSnapshots.main(args);
      DeleteSnapshot.main(args);
      DeleteVolume.main(args);
      DeleteServer.main(args);

      CreateLoadBalancerWithExistingServers.main(args);
      UpdateLoadBalancers.main(args);
      ListLoadBalancers.main(args);
      AddNodes.main(args);
      UpdateNodes.main(args);
      RemoveNodes.main(args);
      CreateLoadBalancerWithNewServers.main(args);
      DeleteServer.main(args);
      DeleteLoadBalancers.main(args);
      
      CreateDomains.main(args);
      ListDomains.main(args);
      UpdateDomains.main(args);
      CreateRecords.main(args);
      ListRecords.main(args);
      UpdateRecords.main(args);
      DeleteRecords.main(args);
      CRUDReverseDNSRecords.main(args);
      DeleteServer.main(args);
      DeleteDomains.main(args);
   }
}
