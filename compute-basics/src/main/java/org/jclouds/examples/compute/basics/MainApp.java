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

package org.jclouds.examples.compute.basics;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.contains;
import static com.google.common.collect.Iterables.getOnlyElement;
import static org.jclouds.aws.ec2.reference.AWSEC2Constants.PROPERTY_EC2_AMI_QUERY;
import static org.jclouds.aws.ec2.reference.AWSEC2Constants.PROPERTY_EC2_CC_AMI_QUERY;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE;
import static org.jclouds.compute.options.TemplateOptions.Builder.overrideLoginCredentials;
import static org.jclouds.compute.options.TemplateOptions.Builder.runScript;
import static org.jclouds.compute.predicates.NodePredicates.TERMINATED;
import static org.jclouds.compute.predicates.NodePredicates.inGroup;
import static org.jclouds.scriptbuilder.domain.Statements.exec;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jclouds.ContextBuilder;
import org.jclouds.apis.ApiMetadata;
import org.jclouds.apis.Apis;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.RunScriptOnNodesException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.googlecloud.GoogleCredentialsFromJson;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.Providers;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.base.Charsets;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Module;

/**
 * Demonstrates the use of {@link ComputeService}.
 * <p/>
 * Usage is:
 * {@code java MainApp provider identity credential groupName (add|exec|run|destroy)}
 * if {@code exec} is used, the following parameter is a command, which should
 * be passed in quotes
 * if {@code run} is used, the following parameter is a file to execute.
 */
public class MainApp {

   public static enum Action {
      ADD, RUN, EXEC, DESTROY, LISTIMAGES, LISTNODES;
   }

   public static final Map<String, ApiMetadata> allApis = Maps.uniqueIndex(Apis.viewableAs(ComputeServiceContext.class),
        Apis.idFunction());

   public static final Map<String, ProviderMetadata> appProviders = Maps.uniqueIndex(Providers.viewableAs(ComputeServiceContext.class),
        Providers.idFunction());

   public static final Set<String> allKeys = ImmutableSet.copyOf(Iterables.concat(appProviders.keySet(), allApis.keySet()));

   public static int PARAMETERS = 5;
   public static String INVALID_SYNTAX = "Invalid number of parameters. Syntax is: provider identity credential groupName (add|exec|run|destroy)";

   public static final String PROPERTY_OAUTH_ENDPOINT = "oauth.endpoint";

   public static void main(String[] args) {
      if (args.length < PARAMETERS)
         throw new IllegalArgumentException(INVALID_SYNTAX);

      String provider = args[0];
      String identity = args[1];
      String credential = args[2];
      String groupName = args[3];
      Action action = Action.valueOf(args[4].toUpperCase());
      boolean providerIsGCE = provider.equalsIgnoreCase("google-compute-engine");

      if (action == Action.EXEC && args.length < PARAMETERS + 1)
         throw new IllegalArgumentException("please quote the command to exec as the last parameter");
      String command = (action == Action.EXEC) ? args[5] : "echo hello";

      // For GCE, the credential parameter is the path to the private key file
      if (providerIsGCE)
         credential = getCredentialFromJsonKeyFile(credential);

      if (action == Action.RUN && args.length < PARAMETERS + 1)
         throw new IllegalArgumentException("please pass the local file to run as the last parameter");
      File file = null;
      if (action == Action.RUN) {
         file = new File(args[5]);
         if (!file.exists())
            throw new IllegalArgumentException("file must exist! " + file);
      }

      String minRam = System.getProperty("minRam");

      // note that you can check if a provider is present ahead of time
      checkArgument(contains(allKeys, provider), "provider %s not in supported list: %s", provider, allKeys);

      LoginCredentials login = (action != Action.DESTROY) ? getLoginForCommandExecution(action) : null;

      ComputeService compute = initComputeService(provider, identity, credential);

      try {
         switch (action) {
         case ADD:
            System.out.printf(">> adding node to group %s%n", groupName);

            // Default template chooses the smallest size on an operating system
            // that tested to work with java, which tends to be Ubuntu or CentOS
            TemplateBuilder templateBuilder = compute.templateBuilder();

            // If you want to up the ram and leave everything default, you can
            // just tweak minRam
            if (minRam != null)
               templateBuilder.minRam(Integer.parseInt(minRam));


            // note this will create a user with the same name as you on the
            // node. ex. you can connect via ssh publicip
            Statement bootInstructions = AdminAccess.standard();

            // to run commands as root, we use the runScript option in the template.
            templateBuilder.options(runScript(bootInstructions));

            Template template = templateBuilder.build();

            NodeMetadata node = getOnlyElement(compute.createNodesInGroup(groupName, 1, template));
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
                  overrideLoginCredentials(login) // use my local user &
                                                 // ssh key
                        .runAsRoot(false) // don't attempt to run as root (sudo)
                        .wrapInInitScript(false));// run command directly

            for (Entry<? extends NodeMetadata, ExecResponse> response : responses.entrySet()) {
               System.out.printf("<< node %s: %s%n", response.getKey().getId(),
                     concat(response.getKey().getPrivateAddresses(), response.getKey().getPublicAddresses()));
               System.out.printf("<<     %s%n", response.getValue());
            }
            break;
         case RUN:
            System.out.printf(">> running [%s] on group %s as %s%n", file, groupName, login.identity);

            // when running a sequence of commands, you probably want to have jclouds use the default behavior,
            // which is to fork a background process.
            responses = compute.runScriptOnNodesMatching(//
                  inGroup(groupName),
                  Files.toString(file, Charsets.UTF_8), // passing in a string with the contents of the file
                  overrideLoginCredentials(login)
                        .runAsRoot(false)
                        .nameTask("_" + file.getName().replaceAll("\\..*", ""))); // ensuring task name isn't
                                                       // the same as the file so status checking works properly

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
         case LISTIMAGES:
            Set<? extends Image> images = compute.listImages();
            System.out.printf(">> No of images %d%n", images.size());
            for (Image img : images) {
               System.out.println(">>>>  " + img);
            }
            break;
         case LISTNODES:
            Set<? extends ComputeMetadata> nodes = compute.listNodes();
            System.out.printf(">> No of nodes/instances %d%n", nodes.size());
            for (ComputeMetadata nodeData : nodes) {
               System.out.println(">>>>  " + nodeData);
            }
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

   private static String getCredentialFromJsonKeyFile(String filename) {
      try {
         String fileContents = Files.toString(new File(filename), UTF_8);
         Supplier<Credentials> credentialSupplier = new GoogleCredentialsFromJson(fileContents);
         String credential = credentialSupplier.get().credential;
         return credential;
      } catch (IOException e) {
         System.err.println("Exception reading private key from '%s': " + filename);
         e.printStackTrace();
         System.exit(1);
         return null;
      }
   }

   static int error = 0;

   private static ComputeService initComputeService(String provider, String identity, String credential) {

      // example of specific properties, in this case optimizing image list to
      // only amazon supplied
      Properties properties = new Properties();
      properties.setProperty(PROPERTY_EC2_AMI_QUERY, "owner-id=137112412989;state=available;image-type=machine");
      properties.setProperty(PROPERTY_EC2_CC_AMI_QUERY, "");
      long scriptTimeout = TimeUnit.MILLISECONDS.convert(20, TimeUnit.MINUTES);
      properties.setProperty(TIMEOUT_SCRIPT_COMPLETE, scriptTimeout + "");

      // set oauth endpoint property if set in system property
      String oAuthEndpoint = System.getProperty(PROPERTY_OAUTH_ENDPOINT);
      if (oAuthEndpoint != null) {
         properties.setProperty(PROPERTY_OAUTH_ENDPOINT, oAuthEndpoint);
      }

      // example of injecting a ssh implementation
      Iterable<Module> modules = ImmutableSet.<Module> of(
            new SshjSshClientModule(),
            new SLF4JLoggingModule(),
            new EnterpriseConfigurationModule());

      ContextBuilder builder = ContextBuilder.newBuilder(provider)
                                             .credentials(identity, credential)
                                             .modules(modules)
                                             .overrides(properties);

      System.out.printf(">> initializing %s%n", builder.getApiMetadata());

      return builder.buildView(ComputeServiceContext.class).getComputeService();
   }

   private static LoginCredentials getLoginForCommandExecution(Action action) {
      try {
        String user = System.getProperty("user.name");
        String privateKey = Files.toString(
            new File(System.getProperty("user.home") + "/.ssh/id_rsa"), UTF_8);
        return LoginCredentials.builder().
            user(user).privateKey(privateKey).build();
      } catch (Exception e) {
         System.err.println("error reading ssh key " + e.getMessage());
         System.exit(1);
         return null;
      }
   }

}
