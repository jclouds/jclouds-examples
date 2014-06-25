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
package org.jclouds.examples.rackspace.cloudservers;

import static com.google.common.base.Charsets.UTF_8;
import static org.jclouds.compute.config.ComputeServiceProperties.POLL_INITIAL_PERIOD;
import static org.jclouds.compute.config.ComputeServiceProperties.POLL_MAX_PERIOD;
import static org.jclouds.examples.rackspace.cloudservers.Constants.NAME;
import static org.jclouds.examples.rackspace.cloudservers.Constants.POLL_PERIOD_TWENTY_SECONDS;
import static org.jclouds.examples.rackspace.cloudservers.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudservers.Constants.ZONE;
import static org.jclouds.scriptbuilder.domain.Statements.exec;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v2_0.domain.KeyPair;
import org.jclouds.openstack.nova.v2_0.domain.zonescoped.ZoneAndId;
import org.jclouds.openstack.nova.v2_0.extensions.KeyPairApi;
import org.jclouds.rest.RestContext;
import org.jclouds.scriptbuilder.ScriptBuilder;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.inject.Module;

/**
 * Create a public key in the cloud and write the private key file to the local working directory. The public key and
 * private key together are known as a key pair (see http://en.wikipedia.org/wiki/Public-key_cryptography). This
 * is a security feature that allows you to login to a server using a private key file.
 *
 * Create a server with the public key, use the private key to login to it, and disable password authentication.
 */
public class CreateServerWithKeyPair implements Closeable {
   private final ComputeService computeService;
   private final RestContext<NovaApi, NovaAsyncApi> novaContext;

   private final File keyPairFile = new File(NAME + ".pem");

   /**
    * To get a username and API key see
    * http://jclouds.apache.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      System.out.format("CreateServerWithKeyPair%n");

      CreateServerWithKeyPair createServerWithKeyPair = new CreateServerWithKeyPair(args[0], args[1]);
      NodeMetadata node;

      try {
         createServerWithKeyPair.detectKeyPairExtension();
         KeyPair keyPair = createServerWithKeyPair.createKeyPair();
         node = createServerWithKeyPair.createServer(keyPair);
         createServerWithKeyPair.disablePasswordAuthentication(node);
         createServerWithKeyPair.deleteKeyPair(keyPair);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createServerWithKeyPair.close();
      }
   }

   public CreateServerWithKeyPair(String username, String apiKey) {
      Iterable<Module> modules = ImmutableSet.<Module> of(new SshjSshClientModule());

      // These properties control how often jclouds polls for a status update
      Properties overrides = new Properties();
      overrides.setProperty(POLL_INITIAL_PERIOD, POLL_PERIOD_TWENTY_SECONDS);
      overrides.setProperty(POLL_MAX_PERIOD, POLL_PERIOD_TWENTY_SECONDS);

      ComputeServiceContext context = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .overrides(overrides)
            .modules(modules)
            .buildView(ComputeServiceContext.class);

      computeService = context.getComputeService();
      novaContext = context.unwrap();
   }

   /**
    * Detect that the OpenStack Key Pair Extension is installed on the Rackspace Cloud.
    *
    * This method is not necessary and is here for demonstration purposes only.
    */
   private void detectKeyPairExtension() {
      Optional<? extends KeyPairApi> keyPairApiExtension = novaContext.getApi().getKeyPairExtensionForZone(ZONE);

      if (keyPairApiExtension.isPresent()) {
         System.out.format("  Key Pair Extension Present%n");

         KeyPairApi keyPairApi = keyPairApiExtension.get();

         for (KeyPair keyPair: keyPairApi.list()) {
            System.out.format("    %s%n", keyPair.getName());
         }
      }
   }

   /**
    * Create a public key in the cloud and write the private key file to the local working directory.
    */
   private KeyPair createKeyPair() throws IOException {
      System.out.format("  Create Key Pair%n");

      KeyPairApi keyPairApi = novaContext.getApi().getKeyPairExtensionForZone(ZONE).get();
      KeyPair keyPair = keyPairApi.create(NAME);

      Files.write(keyPair.getPrivateKey(), keyPairFile, UTF_8);

      System.out.format("    Wrote %s%n", keyPairFile.getAbsolutePath());

      return keyPair;
   }

   /**
    * Create a server with the key pair.
    */
   private NodeMetadata createServer(KeyPair keyPair) throws RunNodesException, TimeoutException {
      System.out.format("  Create Server%n");

      NovaTemplateOptions options = NovaTemplateOptions.Builder.keyPairName(keyPair.getName());

      ZoneAndId zoneAndId = ZoneAndId.fromZoneAndId(ZONE, "performance1-1");
      Template template = computeService.templateBuilder()
            .locationId(ZONE)
            .osDescriptionMatches(".*Ubuntu 12.04.*")
            .hardwareId(zoneAndId.slashEncode())
            .options(options)
            .build();

      // This method will continue to poll for the server status and won't return until this server is ACTIVE
      // If you want to know what's happening during the polling, enable logging.
      // See /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      Set<? extends NodeMetadata> nodes = computeService.createNodesInGroup(NAME, 1, template);
      NodeMetadata node = Iterables.getOnlyElement(nodes);

      System.out.format("    %s%n", node);

      return node;
   }

   /**
    * If desired, you can disable password authentication for the server because we can use a private key to SSH in.
    *
    * No need to explicitly include the private key with running the script on the node. jclouds is already aware
    * of the private key when the node was created earlier.
    */
   private void disablePasswordAuthentication(NodeMetadata node) throws TimeoutException {
      System.out.format("  Disable Password Authentication%n");

      String script = new ScriptBuilder()
            .addStatement(exec("sed -i 's/#PasswordAuthentication yes/PasswordAuthentication no/g' /etc/ssh/sshd_config"))
            .addStatement(exec("service ssh restart"))
            .render(OsFamily.UNIX);

      RunScriptOptions options = RunScriptOptions.Builder
            .blockOnPort(22, 10)
            .blockOnComplete(true);

      computeService.runScriptOnNode(node.getId(), script, options);

      String publicAddress = Iterables.getOnlyElement(node.getPublicAddresses());

      System.out.format("    ssh -i %s root@%s%n", keyPairFile.getAbsolutePath(), publicAddress);
   }

   /**
    * Delete the public key in the cloud and the local private key.
    */
   private void deleteKeyPair(KeyPair keyPair) {
      System.out.format("  Delete Key Pair%n");

      KeyPairApi keyPairApi = novaContext.getApi().getKeyPairExtensionForZone(ZONE).get();
      keyPairApi.delete(keyPair.getName());

      if (keyPairFile.delete()) {
         System.out.format("    Deleted %s%n", keyPairFile.getAbsolutePath());
      }
      else {
         System.err.format("    Could not delete %s%n", keyPairFile.getAbsolutePath());
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(computeService.getContext(), true);
   }
}
