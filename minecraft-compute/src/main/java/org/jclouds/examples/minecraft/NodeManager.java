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

package org.jclouds.examples.minecraft;

import static com.google.common.base.Predicates.not;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Sets.filter;
import static org.jclouds.compute.predicates.NodePredicates.TERMINATED;
import static org.jclouds.compute.predicates.NodePredicates.all;
import static org.jclouds.compute.predicates.NodePredicates.inGroup;
import static org.jclouds.compute.predicates.NodePredicates.runningInGroup;
import static org.jclouds.examples.minecraft.Utils.asCurrentUser;
import static org.jclouds.examples.minecraft.Utils.getStdout;
import static org.jclouds.scriptbuilder.domain.Statements.newStatementList;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.RunScriptOnNodesException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.logging.Logger;
import org.jclouds.scriptbuilder.InitBuilder;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.statements.java.InstallJDK;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;

/**
 * @author Adrian Cole
 */
@Singleton
public class NodeManager {
   @Resource
   private Logger logger = Logger.NULL;

   private final ComputeService compute;

   @Inject
   NodeManager(ComputeService compute) {
      this.compute = compute;
   }

   @SuppressWarnings("unchecked")
   public Map<NodeMetadata, String> stdoutFromCommandOnGroup(String command, String group) {
      try {
         return transformValues((Map<NodeMetadata, ExecResponse>) compute.runScriptOnNodesMatching(
               runningInGroup(group), command, asCurrentUser().wrapInInitScript(false)), getStdout());
      } catch (RunScriptOnNodesException e) {
         throw propagate(e);
      }
   }

   public ExecResponse startDaemonOnNode(InitBuilder daemon, String nodeId) {
      return compute.runScriptOnNode(nodeId, daemon, asCurrentUser().blockOnComplete(false));
   }

   public Set<? extends NodeMetadata> listRunningNodesInGroup(String group) {
      return filter(compute.listNodesDetailsMatching(all()), runningInGroup(group));
   }

   public Set<? extends NodeMetadata> destroyNodesInGroup(String group) {
      return compute.destroyNodesMatching(Predicates.<NodeMetadata> and(inGroup(group), not(TERMINATED)));
   }

   public RuntimeException destroyBadNodesAndPropagate(RunNodesException e) {
      for (Entry<? extends NodeMetadata, ? extends Throwable> nodeError : e.getNodeErrors().entrySet())
         compute.destroyNode(nodeError.getKey().getId());
      throw propagate(e);
   }

   public NodeMetadata createNodeWithAdminUserAndJDKInGroupOpeningPortAndMinRam(String group, int port, int minRam) {
      ImmutableMap<String, String> userMetadata = ImmutableMap.<String, String> of("Name", group);
      
      // we want everything as defaults except ram
      Template defaultTemplate = compute.templateBuilder().build();
      Template minecraft = compute.templateBuilder().fromTemplate(defaultTemplate).minRam(minRam).build();
      
      // setup the template to customize the node with jdk, etc. also opening ports.
      Statement bootstrap = newStatementList(AdminAccess.standard(), InstallJDK.fromURL());
      minecraft.getOptions().inboundPorts(22, port).userMetadata(userMetadata).runScript(bootstrap);

      logger.info(">> creating node type(%s) in group %s, opening ports 22, %s with admin user and jdk", minecraft
            .getHardware().getId(), group, port);

      try {

         NodeMetadata node = getOnlyElement(compute.createNodesInGroup(group, 1, minecraft));

         logger.info("<< available node(%s) os(%s) publicAddresses%s", node.getId(), node.getOperatingSystem(),
               node.getPublicAddresses());
         return node;
      } catch (RunNodesException e) {
         throw destroyBadNodesAndPropagate(e);
      }
   }

   @Override
   public String toString() {
      return String.format("connection(%s@%s)", compute.getContext().getProviderSpecificContext().getIdentity(),
            compute.getContext().getProviderSpecificContext().getEndpoint().toASCIIString());
   }

}
