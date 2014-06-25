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
package org.jclouds.examples.rackspace.autoscale;

import static org.jclouds.examples.rackspace.autoscale.Constants.NAME;

import org.jclouds.rackspace.autoscale.v1.domain.Group;
import org.jclouds.rackspace.autoscale.v1.domain.GroupState;
import org.jclouds.rackspace.autoscale.v1.domain.ScalingPolicy;
import org.jclouds.rackspace.autoscale.v1.features.GroupApi;
import org.jclouds.rackspace.autoscale.v1.features.PolicyApi;

/**
 * Helper methods for autoscale examples
 */
public class Utils {
   public static String getGroupId(GroupApi groupApi) {
      for ( GroupState state : groupApi.listGroupStates() ) {
         Group g = groupApi.get(state.getId());
         for ( ScalingPolicy policy : g.getScalingPolicies() ) {
            if (policy.getName().equals(NAME)) return g.getId();
         }
      }

      throw new IllegalArgumentException("Group not found");
   }

   public static String getPolicyId(PolicyApi policyApi) {
      for ( ScalingPolicy policy : policyApi.list() ) {
         if (policy.getName().equals(NAME)) return policy.getId();
      }

      throw new IllegalArgumentException("Policy not found");
   }
}
