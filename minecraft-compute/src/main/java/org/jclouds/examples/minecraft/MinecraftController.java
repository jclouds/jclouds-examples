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

package org.jclouds.examples.minecraft;

import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.io.Closeables.closeQuietly;
import static org.jclouds.examples.minecraft.Utils.firstPublicAddressToHostAndPort;
import static org.jclouds.util.Maps2.transformKeys;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.lifecycle.Closer;
import org.jclouds.logging.Logger;
import org.jclouds.scriptbuilder.InitScript;

import com.google.common.net.HostAndPort;

@Singleton
public class MinecraftController implements Closeable {

   @Resource
   protected Logger logger = Logger.NULL;

   private final Closer closer;
   private final NodeManager nodeManager;
   private final Provider<InitScript> daemonFactory;
   private final int port;
   private final String group;
   private final int maxHeap;

   @Inject
   MinecraftController(Closer closer, NodeManager nodeManager, Provider<InitScript> daemonFactory,
         @Named("minecraft.port") int port, @Named("minecraft.group") String group,  @Named("minecraft.mx") int maxHeap) {
      this.closer = closer;
      this.nodeManager = nodeManager;
      this.daemonFactory = daemonFactory;
      this.port = port;
      this.group = group;
      this.maxHeap = maxHeap;
   }

   public Iterable<HostAndPort> list() {
      return transformToHostAndPort(nodeManager.listRunningNodesInGroup(group));
   }

   public Iterable<HostAndPort> transformToHostAndPort(Set<? extends NodeMetadata> nodes) {
      return transform(nodes, firstPublicAddressToHostAndPort(port));
   }

   public HostAndPort add() {
      return firstPublicAddressToHostAndPort(port).apply(createNodeWithMinecraft());
   }

   private NodeMetadata createNodeWithMinecraft() {
      int javaPlusOverhead = maxHeap + 256;
      NodeMetadata node = nodeManager.createNodeWithAdminUserAndJDKInGroupOpeningPortAndMinRam(group, port,
            javaPlusOverhead);
      nodeManager.startDaemonOnNode(daemonFactory.get(), node.getId());
      return node;
   }

   public Map<HostAndPort, String> tail() {
      return mapHostAndPortToStdoutForCommand("/tmp/init-minecraft tail");
   }

   public Map<HostAndPort, String> mapHostAndPortToStdoutForCommand(String cmd) {
      return transformKeys(nodeManager.stdoutFromCommandOnGroup(cmd, group), firstPublicAddressToHostAndPort(port));
   }

   public Map<HostAndPort, String> pids() {
      return mapHostAndPortToStdoutForCommand("/tmp/init-minecraft status");
   }

   public Iterable<HostAndPort> destroy() {
      return transformToHostAndPort(nodeManager.destroyNodesInGroup(group));
   }

   @Override
   public void close() {
      closeQuietly(closer);
   }

   @Override
   public String toString() {
      return toStringHelper("").add("nodeManager", nodeManager).add("group", group).add("port", port).toString();
   }
}
