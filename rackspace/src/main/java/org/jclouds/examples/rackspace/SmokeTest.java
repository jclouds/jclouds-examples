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

import java.io.IOException;

import org.jclouds.examples.rackspace.cloudblockstorage.*;
import org.jclouds.examples.rackspace.clouddns.*;
import org.jclouds.examples.rackspace.cloudfiles.*;
import org.jclouds.examples.rackspace.cloudloadbalancers.*;
import org.jclouds.examples.rackspace.cloudqueues.ProducerConsumer;
import org.jclouds.examples.rackspace.cloudqueues.PublishSubscribe;
import org.jclouds.examples.rackspace.cloudqueues.StreamMessages;
import org.jclouds.examples.rackspace.cloudservers.*;
import org.jclouds.examples.rackspace.clouddatabases.*;
import org.jclouds.examples.rackspace.autoscale.*;

/**
 * This example smoke tests all of the other examples in these packages.
 */
public class SmokeTest {

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      SmokeTest smokeTest = new SmokeTest();
      smokeTest.smokeTest(args);
   }

   private void smokeTest(String[] args) throws IOException {
      Authentication.main(args);
      Logging.main(args);

      CloudServersPublish.main(args);
      CreateServer.main(args);
      CreateServerWithKeyPair.main(args);
      ListServersWithFiltering.main(args);
      ServerMetadata.main(args);
      DeleteServer.main(args);

      CloudFilesPublish.main(args);
      CreateContainer.main(args);
      ListContainers.main(args);
      GenerateTempURL.main(args);
      UploadObjects.main(args);
      ListObjects.main(args);
      CrossOriginResourceSharingContainer.main(args);
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

      CreateInstance.main(args);
      CreateDatabase.main(args);
      CreateUser.main(args);
      TestDatabase.main(args);
      GrantRootAccess.main(args);
      DeleteDatabase.main(args);
      DeleteUser.main(args);
      DeleteInstance.main(args);

      CreatePolicy.main(args);
      UpdatePolicy.main(args);
      CreateWebhook.main(args);
      ExecuteWebhook.main(args);
      AutoscaleCleanup.main(args);

      ProducerConsumer.main(args);
      PublishSubscribe.main(args);
      StreamMessages.main(args);
   }
}
