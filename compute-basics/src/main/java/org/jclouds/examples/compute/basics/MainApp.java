/**
 *
 * Copyright (C) 2011 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.jclouds.examples.compute.basics;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.contains;
import static com.google.common.collect.Iterables.getOnlyElement;
import static org.jclouds.compute.options.TemplateOptions.Builder.overrideCredentialsWith;
import static org.jclouds.compute.options.TemplateOptions.Builder.runScript;
import static org.jclouds.compute.predicates.NodePredicates.TERMINATED;
import static org.jclouds.compute.predicates.NodePredicates.inGroup;
import static org.jclouds.scriptbuilder.domain.Statements.exec;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.RunScriptOnNodesException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.util.ComputeServiceUtils;
import org.jclouds.domain.Credentials;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;
import org.jclouds.ssh.jsch.config.JschSshClientModule;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.google.inject.Module;

/**
 * Demonstrates the use of {@link ComputeService}.
 * <p/>
 * Usage is:
 * {@code java MainApp provider identity credential groupName (add|exec|destroy)}
 * if {@code exec} is used, the following parameter is a command, which should
 * be passed in quotes
 * 
 * @author Adrian Cole
 */
public class MainApp {

   public static enum Action {
      ADD, EXEC, DESTROY;
   }

   public static int PARAMETERS = 5;
   public static String INVALID_SYNTAX = "Invalid number of parameters. Syntax is: provider identity credential groupName (add|exec|destroy)";

   public static void main(String[] args) {
      if (args.length < PARAMETERS)
         throw new IllegalArgumentException(INVALID_SYNTAX);

      String provider = args[0];
      String identity = args[1];
      String credential = args[2];
      String groupName = args[3];
      Action action = Action.valueOf(args[4].toUpperCase());
      if (action == Action.EXEC && args.length < PARAMETERS + 1)
         throw new IllegalArgumentException("please quote the command to exec as the last parameter");
      String command = (action == Action.EXEC) ? args[5] : "echo hello";

      // note that you can check if a provider is present ahead of time
      if (!contains(ComputeServiceUtils.getSupportedProviders(), provider))
         throw new IllegalArgumentException("provider " + provider + " not in supported list: "
               + ComputeServiceUtils.getSupportedProviders());

      Credentials login = (action != Action.DESTROY) ? getLoginForCommandExecution(action) : null;

      ComputeService compute = initComputeService(provider, identity, credential);

      System.out.printf(">> initialized provider %s%n", compute.getContext().getProviderSpecificContext());

      try {
         switch (action) {
         case ADD:
            System.out.printf(">> adding node to group %s%n", groupName);
            // note this will create a user with the same name as you on the
            // node. ex. you can connect via ssh publicip
            Statement bootInstructions = AdminAccess.standard();

            // in this case, we don't care about template option such as
            // operating system, or hardware profile. jclouds will select one
            // for us, which tends to be Ubuntu or CentOS
            NodeMetadata node = getOnlyElement(compute.createNodesInGroup(groupName, 1, runScript(bootInstructions)));
            System.out.printf("<< node %s: %s%n", node.getId(),
                  concat(node.getPrivateAddresses(), node.getPublicAddresses()));

         case EXEC:
            System.out.printf(">> running [%s] on group %s as %s%n", command, groupName, login.identity);

            // when you run commands, you can pass options to decide whether to
            // run it as root, supply or own credentials vs from cache, and wrap
            // in an init script vs directly invoke
            Map<? extends NodeMetadata, ExecResponse> responses = compute.runScriptOnNodesMatching(//
                  inGroup(groupName), // predicate used to select nodes
                  exec(command), // what you actually intend to run
                  overrideCredentialsWith(login) // use my local user &
                                                 // ssh key
                        .runAsRoot(false) // don't attempt to run as root (sudo)
                        .wrapInInitScript(false));// run command directly

            for (Entry<? extends NodeMetadata, ExecResponse> response : responses.entrySet()) {
               System.out.printf("<< node %s: %s%n", response.getKey().getId(),
                     concat(response.getKey().getPrivateAddresses(), response.getKey().getPublicAddresses()));
               System.out.printf("<<     %s%n", response.getValue());
            }
            break;
         case DESTROY:
            System.out.printf(">> destroying nodes in group %s%n", groupName);
            // you can use predicates to select which nodes you wish to destroy.
            Set<? extends NodeMetadata> destroyed = compute.destroyNodesMatching(//
                  Predicates.<NodeMetadata> and(not(TERMINATED), inGroup(groupName)));
            System.out.printf("<< destroyed nodes %s%n", destroyed);
            break;
         }
      } catch (RunNodesException e) {
         System.err.println("error adding node to group " + groupName + ": " + e.getMessage());
         error = 1;
      } catch (RunScriptOnNodesException e) {
         System.err.println("error executing " + command + " on group " + groupName + ": " + e.getMessage());
         error = 1;
      } catch (Exception e) {
         System.err.println("error: " + e.getMessage());
         error = 1;
      } finally {
         compute.getContext().close();
         System.exit(error);
      }
   }

   static int error = 0;

   private static ComputeService initComputeService(String provider, String identity, String credential) {
      // example of specific properties, in this case optimizing image list to
      // only amazon supplied
      Properties properties = new Properties();
      properties.setProperty("jclouds.ec2.ami-owners", "137112412989");

      // example of injecting a ssh implementation
      Iterable<Module> modules = ImmutableSet.<Module> of(new JschSshClientModule());

      return new ComputeServiceContextFactory().createContext(provider, identity, credential, modules, properties)
            .getComputeService();
   }

   private static Credentials getLoginForCommandExecution(Action action) {
      try {
         return new Credentials(System.getProperty("user.name"), Files.toString(
               new File(System.getProperty("user.home") + "/.ssh/id_rsa"), UTF_8));
      } catch (Exception e) {
         System.err.println("error reading ssh key " + e.getMessage());
         System.exit(1);
         return null;
      }
   }

}
