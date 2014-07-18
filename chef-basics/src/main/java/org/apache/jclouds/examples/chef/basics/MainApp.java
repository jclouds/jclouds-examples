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

package org.apache.jclouds.examples.chef.basics;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.contains;
import static com.google.common.collect.Iterables.getOnlyElement;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE;
import static org.jclouds.compute.options.TemplateOptions.Builder.overrideLoginCredentials;
import static org.jclouds.compute.options.TemplateOptions.Builder.runScript;
import static org.jclouds.compute.predicates.NodePredicates.TERMINATED;
import static org.jclouds.compute.predicates.NodePredicates.inGroup;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jclouds.ContextBuilder;
import org.jclouds.apis.ApiMetadata;
import org.jclouds.apis.Apis;
import org.jclouds.chef.ChefApiMetadata;
import org.jclouds.chef.ChefContext;
import org.jclouds.chef.ChefService;
import org.jclouds.chef.config.ChefProperties;
import org.jclouds.chef.domain.BootstrapConfig;
import org.jclouds.chef.util.RunListBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.RunScriptOnNodesException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.Providers;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.StatementList;
import org.jclouds.scriptbuilder.domain.chef.RunList;
import org.jclouds.scriptbuilder.statements.chef.ChefSolo;
import org.jclouds.scriptbuilder.statements.chef.InstallChefUsingOmnibus;
import org.jclouds.scriptbuilder.statements.git.CloneGitRepo;
import org.jclouds.scriptbuilder.statements.git.InstallGit;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Module;

/**
 * Demonstrates the use of {@link ComputeService}.
 * <p/>
 * Usage is: {@code java MainApp provider identity credential groupName (add|chef|destroy)} if
 * {@code chef} is used, the following parameter is a list of recipes to be installed in the node
 * separated by commas.
 */
public class MainApp {

    public static enum Action {
        ADD, CHEF, SOLO, DESTROY;
    }

    public static final Map<String, ApiMetadata> allApis = Maps.uniqueIndex(
            Apis.viewableAs(ComputeServiceContext.class), Apis.idFunction());

    public static final Map<String, ProviderMetadata> appProviders = Maps.uniqueIndex(
            Providers.viewableAs(ComputeServiceContext.class), Providers.idFunction());

    public static final Set<String> allKeys = ImmutableSet.copyOf(Iterables.concat(
            appProviders.keySet(), allApis.keySet()));

    public static int PARAMETERS = 5;

    public static String INVALID_SYNTAX =
            "Invalid number of parameters. Syntax is: provider identity credential groupName (add|chef|solo|destroy)";

    public static void main(final String[] args) {
        if (args.length < PARAMETERS) {
            throw new IllegalArgumentException(INVALID_SYNTAX);
        }

        String provider = args[0];
        String identity = args[1];
        String credential = args[2];
        String groupName = args[3];
        Action action = Action.valueOf(args[4].toUpperCase());
        if ((action == Action.CHEF || action == Action.SOLO) && args.length < PARAMETERS + 1) {
            throw new IllegalArgumentException("please provide the list of recipes to install, separated by commas");
        }
        String recipes = action == Action.CHEF || action == Action.SOLO ? args[5] : "apache2";

        String minRam = System.getProperty("minRam");

        // note that you can check if a provider is present ahead of time
        checkArgument(contains(allKeys, provider), "provider %s not in supported list: %s",
                provider, allKeys);

        LoginCredentials login =
                action != Action.DESTROY ? getLoginForCommandExecution(action) : null;

        ComputeService compute = initComputeService(provider, identity, credential);

        try {
            switch (action) {
                case ADD:
                    System.out.printf(">> adding node to group %s%n", groupName);

                    // Default template chooses the smallest size on an operating
                    // system that tested to work with java
                    TemplateBuilder templateBuilder = compute.templateBuilder();
                    templateBuilder.osFamily(OsFamily.UBUNTU);

                    // If you want to up the ram and leave everything default, you
                    // can just tweak minRam
                    if (minRam != null) {
                        templateBuilder.minRam(Integer.parseInt(minRam));
                    }

                    // note this will create a user with the same name as you on the
                    // node. ex. you can connect via ssh publicip
                    Statement bootInstructions = AdminAccess.standard();

                    // to run commands as root, we use the runScript option in the
                    // template.
                    templateBuilder.options(runScript(bootInstructions));

                    NodeMetadata node =
                            getOnlyElement(compute.createNodesInGroup(groupName, 1,
                                    templateBuilder.build()));
                    System.out.printf("<< node %s: %s%n", node.getId(),
                            concat(node.getPrivateAddresses(), node.getPublicAddresses()));

                case SOLO:
                    System.out.printf(">> installing [%s] on group %s as %s%n", recipes, groupName,
                            login.identity);

                    Iterable<String> recipeList = Splitter.on(',').split(recipes);
                    ImmutableList.Builder<Statement> bootstrapBuilder = ImmutableList.builder();
                    bootstrapBuilder.add(new InstallGit());

                    // Clone community cookbooks into the node
                    for (String recipe : recipeList) {
                        bootstrapBuilder.add(CloneGitRepo.builder()
                                .repository("git://github.com/opscode-cookbooks/" + recipe + ".git")
                                .directory("/var/chef/cookbooks/" + recipe) //
                                .build());
                    }

                    // Configure Chef Solo to bootstrap the selected recipes
                    bootstrapBuilder.add(new InstallChefUsingOmnibus());
                    bootstrapBuilder.add(ChefSolo.builder() //
                            .cookbookPath("/var/chef/cookbooks") //
                            .runlist(RunList.builder().recipes(recipeList).build()) //
                            .build());

                    // Build the statement that will perform all the operations above
                    StatementList bootstrap = new StatementList(bootstrapBuilder.build());

                    // Run the script in the nodes of the group
                    runScriptOnGroup(compute, login, groupName, bootstrap);
                    break;
                case CHEF:
                    // Create the connection to the Chef server
                    ChefService chef =
                            initChefService(System.getProperty("chef.client"),
                                    System.getProperty("chef.validator"));

                    // Build the runlist for the deployed nodes
                    System.out.println("Configuring node runlist in the Chef server...");
                    List<String> runlist =
                            new RunListBuilder().addRecipes(recipes.split(",")).build();
                    BootstrapConfig config = BootstrapConfig.builder().runList(runlist).build();
                    chef.updateBootstrapConfigForGroup(groupName, config);
                    Statement chefServerBootstrap = chef.createBootstrapScriptForGroup(groupName);

                    // Run the script in the nodes of the group
                    System.out.printf(">> installing [%s] on group %s as %s%n", recipes, groupName,
                            login.identity);
                    runScriptOnGroup(compute, login, groupName, chefServerBootstrap);
                    break;
                case DESTROY:
                    System.out.printf(">> destroying nodes in group %s%n", groupName);
                    // you can use predicates to select which nodes you wish to
                    // destroy.
                    Set<? extends NodeMetadata> destroyed = compute.destroyNodesMatching(//
                            Predicates.<NodeMetadata>and(not(TERMINATED), inGroup(groupName)));
                    System.out.printf("<< destroyed nodes %s%n", destroyed);
                    break;
            }
        } catch (RunNodesException e) {
            System.err.println("error adding node to group " + groupName + ": " + e.getMessage());
            error = 1;
        } catch (RunScriptOnNodesException e) {
            System.err.println("error installing " + recipes + " on group " + groupName + ": "
                    + e.getMessage());
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

    private static void runScriptOnGroup(final ComputeService compute,
                                         final LoginCredentials login, final String groupName, final Statement command)
            throws RunScriptOnNodesException {
        // when you run commands, you can pass options to decide whether
        // to run it as root, supply or own credentials vs from cache,
        // and wrap in an init script vs directly invoke
        Map<? extends NodeMetadata, ExecResponse> execResponses =
                compute.runScriptOnNodesMatching(//
                        inGroup(groupName), // predicate used to select nodes
                        command, // what you actually intend to run
                        overrideLoginCredentials(login)); // use the local user & ssh key

        for (Entry<? extends NodeMetadata, ExecResponse> response : execResponses.entrySet()) {
            System.out.printf(
                    "<< node %s: %s%n",
                    response.getKey().getId(),
                    concat(response.getKey().getPrivateAddresses(), response.getKey()
                            .getPublicAddresses())
            );
            System.out.printf("<<     %s%n", response.getValue());
        }
    }

    private static ComputeService initComputeService(final String provider, final String identity,
                                                     final String credential) {
        // example of specific properties, in this case optimizing image list to
        // only amazon supplied
        Properties properties = new Properties();
        long scriptTimeout = TimeUnit.MILLISECONDS.convert(20, TimeUnit.MINUTES);
        properties.setProperty(TIMEOUT_SCRIPT_COMPLETE, scriptTimeout + "");

        // example of injecting a ssh implementation
        Iterable<Module> modules =
                ImmutableSet.<Module>of(new SshjSshClientModule(), new SLF4JLoggingModule(),
                        new EnterpriseConfigurationModule());

        ContextBuilder builder =
                ContextBuilder.newBuilder(provider).credentials(identity, credential).modules(modules)
                        .overrides(properties);

        System.out.printf(">> initializing %s%n", builder.getApiMetadata());

        return builder.buildView(ComputeServiceContext.class).getComputeService();
    }

    private static ChefService initChefService(final String client, final String validator) {
        try {
            Properties chefConfig = new Properties();
            chefConfig.put(ChefProperties.CHEF_VALIDATOR_NAME, validator);
            chefConfig
                    .put(ChefProperties.CHEF_VALIDATOR_CREDENTIAL, credentialForClient(validator));

            ContextBuilder builder = ContextBuilder.newBuilder(new ChefApiMetadata()) //
                    .credentials(client, credentialForClient(client)) //
                    .modules(ImmutableSet.<Module>of(new SLF4JLoggingModule())) //
                    .overrides(chefConfig); //

            System.out.printf(">> initializing %s%n", builder.getApiMetadata());

            ChefContext context = builder.buildView(ChefContext.class);
            return context.getChefService();
        } catch (Exception e) {
            System.err.println("error reading private key " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    private static LoginCredentials getLoginForCommandExecution(final Action action) {
        try {
            String user = System.getProperty("user.name");
            String privateKey =
                    Files.toString(new File(System.getProperty("user.home") + "/.ssh/id_rsa"), UTF_8);
            return LoginCredentials.builder().user(user).privateKey(privateKey)
                    .authenticateSudo(true).build();
        } catch (Exception e) {
            System.err.println("error reading ssh key " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    private static String credentialForClient(final String client) throws Exception {
        String pemFile = System.getProperty("user.home") + "/.chef/" + client + ".pem";
        return Files.toString(new File(pemFile), UTF_8);
    }

}
