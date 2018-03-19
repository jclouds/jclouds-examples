/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.examples.rackspace.cloudnetworks;

import static org.jclouds.examples.rackspace.cloudnetworks.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudnetworks.Constants.REGION;

import java.io.Closeable;
import java.io.IOException;

import org.jclouds.ContextBuilder;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.Rule;
import org.jclouds.openstack.neutron.v2.domain.RuleDirection;
import org.jclouds.openstack.neutron.v2.domain.RuleEthertype;
import org.jclouds.openstack.neutron.v2.domain.RuleProtocol;
import org.jclouds.openstack.neutron.v2.domain.SecurityGroup;

import org.jclouds.openstack.neutron.v2.features.SecurityGroupApi;
import com.google.common.io.Closeables;

/**
 * Demonstrates how to create a Poppy service on Rackspace (Rackspace CDN).
 * Cleans up the service on fail.
 */
public class CreateSecurityGroup implements Closeable {
   private final NeutronApi neutronApi;

   /**
    * To get a username and API key see
    * http://jclouds.apache.org/guides/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      CreateSecurityGroup createSecurityGroup = new CreateSecurityGroup(args[0], args[1]);

      try {
         createSecurityGroup.createSecurityGroup();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createSecurityGroup.close();
      }
   }

   public CreateSecurityGroup(String username, String apiKey) {
      neutronApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(NeutronApi.class);
   }

   private void createSecurityGroup() {
      SecurityGroupApi sgApi = neutronApi.getSecurityGroupApi(REGION);
      Rule rule = null;
      SecurityGroup securityGroup = null;

      try {
         sgApi = neutronApi.getSecurityGroupApi(REGION);

         securityGroup = sgApi.create(
               SecurityGroup.createBuilder().name("jclouds-test").description("jclouds test security group")
                     .build());

         rule = sgApi.create(
               Rule.createBuilder(RuleDirection.INGRESS, securityGroup.getId())
                     .ethertype(RuleEthertype.IPV6)
                     .portRangeMax(90)
                     .portRangeMin(80)
                     .protocol(RuleProtocol.TCP)
                     .build());
      } finally {
         // Cleanup
         if (rule != null) {
            sgApi.deleteRule(rule.getId());
         }
         if (securityGroup != null) {
            sgApi.deleteSecurityGroup(securityGroup.getId());
         }
      }
   }

   /**
    * Always close your service when you're done with it.
    *
    * Note that closing quietly like this is not necessary in Java 7.
    * You would use try-with-resources in the main method instead.
    */
   public void close() throws IOException {
      Closeables.close(neutronApi, true);
   }
}
