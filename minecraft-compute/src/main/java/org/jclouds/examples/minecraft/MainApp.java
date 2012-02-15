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

import static com.google.common.collect.Iterables.contains;
import static org.jclouds.location.reference.LocationConstants.PROPERTY_REGIONS;

import java.util.Map;
import java.util.Properties;

import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.util.ComputeServiceUtils;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;
import com.google.inject.Module;

/**
 * Demonstrates control of Minecraft.
 * <p/>
 * Usage is:
 * {@code java MainApp provider identity credential groupName (add|list|tail|pids|destroy)}
 * 
 * @author Adrian Cole
 */
public class MainApp {

   public static enum Action {
      ADD, LIST, TAIL, PIDS, DESTROY;
   }

   public static int PARAMETERS = 5;
   public static String INVALID_SYNTAX = "Invalid number of parameters. Syntax is: provider identity credential groupName (add|list|tail|pids|destroy)";

   public static void main(String[] args) {
      if (args.length < PARAMETERS)
         throw new IllegalArgumentException(INVALID_SYNTAX);

      String provider = args[0];
      String identity = args[1];
      String credential = args[2];
      String groupName = args[3];
      Action action = Action.valueOf(args[4].toUpperCase());

      // note that you can check if a provider is present ahead of time
      if (!contains(ComputeServiceUtils.getSupportedProviders(), provider))
         throw new IllegalArgumentException("provider " + provider + " not in supported list: "
               + ComputeServiceUtils.getSupportedProviders());

      MinecraftController controller = initController(provider, identity, credential, groupName);

      System.out.printf(">> initialized controller %s%n", controller);

      try {
         switch (action) {
         case ADD:
            System.out.printf(">> adding a server to group %s%n", groupName);
            HostAndPort server = controller.add();
            System.out.printf("<< server %s%n", server);
            break;
         case LIST:
            System.out.printf(">> listing servers in group %s%n", groupName);
            Iterable<HostAndPort> servers = controller.list();
            System.out.printf("<< servers %s%n", servers);
            break;
         case TAIL:
            System.out.printf(">> tailing all servers in group %s%n", groupName);
            Map<HostAndPort, String> output = controller.tail();
            System.out.printf("<< output %s%n", output);
            break;
         case PIDS:
            System.out.printf(">> getting pids of all servers in group %s%n", groupName);
            Map<HostAndPort, String> pids = controller.pids();
            System.out.printf("<< pids %s%n", pids);
            break;
         case DESTROY:
            System.out.printf(">> destroying group %s%n", groupName);
            Iterable<HostAndPort> destroyed = controller.destroy();
            System.out.printf("<< destroyed servers %s%n", destroyed);
            break;
         }
      } catch (RuntimeException e) {
         error = 1;
         e.printStackTrace();
      } finally {
         controller.close();
         System.exit(error);
      }
   }

   static int error = 0;

   private static MinecraftController initController(String provider, String identity, String credential, String group) {
      Properties properties = new Properties();
      properties.setProperty("minecraft.port", "25565");
      properties.setProperty("minecraft.group", group);
      properties.setProperty("minecraft.ms", "512");
      properties.setProperty("minecraft.mx", "512");
      properties.setProperty("minecraft.url",
            "https://s3.amazonaws.com/MinecraftDownload/launcher/minecraft_server.jar");
      if ("aws-ec2".equals(provider)) {
         // since minecraft download is in s3 on us-east, lowest latency is from
         // there
         properties.setProperty(PROPERTY_REGIONS, "us-east-1");
         properties.setProperty("jclouds.ec2.ami-query", "owner-id=137112412989;state=available;image-type=machine");
         properties.setProperty("jclouds.ec2.cc-ami-query", "");
      }
      // example of injecting a ssh implementation
      Iterable<Module> modules = ImmutableSet.<Module> of(new SshjSshClientModule(), new SLF4JLoggingModule(),
            new EnterpriseConfigurationModule());

      return new ComputeServiceContextFactory().createContext(provider, identity, credential, modules, properties)
            .utils().injector().getInstance(MinecraftController.class);
   }

}
