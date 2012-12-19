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
package org.jclouds.examples.rackspace.cloudservers;

import static com.google.common.io.Closeables.closeQuietly;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jclouds.scriptbuilder.domain.Statements.exec;
import static org.jclouds.util.Predicates2.retry;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.config.ComputeServiceProperties;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.predicates.SocketOpen;
import org.jclouds.scriptbuilder.ScriptBuilder;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;
import com.google.inject.Module;

/**
 * This example creates a server, start a web server on it, and publish a page on the internet!
 */
public class CloudServersPublish implements Closeable {
   private ComputeService compute;
   int numServers;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username 
    * The second argument (args[1]) must be your API key
    * The optional third argument (args[1]) is the number of Cloud Servers to start
    */
   public static void main(String[] args) {
      getPublishedCloudServers(Arrays.asList(args));
   }

   public static Set<? extends NodeMetadata> getPublishedCloudServers(List<String> args) {
      CloudServersPublish cloudServersPublish = new CloudServersPublish();
      Set<? extends NodeMetadata> nodes = null;
      
      try {
         cloudServersPublish.init(args);
         nodes = cloudServersPublish.createServer();
         cloudServersPublish.configureAndStartWebserver(nodes);
         
         return nodes;
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         cloudServersPublish.close();
      }
      
      return nodes;
   }

   private void init(List<String> args) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the provider to "rackspace-cloudservers-uk"
      String provider = "rackspace-cloudservers-us"; 

      String username = args.get(0);
      String apiKey = args.get(1);
      numServers = args.size() == 3 ? Integer.valueOf(args.get(2)) : 1;

      Iterable<Module> modules = ImmutableSet.<Module> of(new SshjSshClientModule());

      // These properties control how often jclouds polls for a status udpate
      Properties overrides = new Properties();
      overrides.setProperty(ComputeServiceProperties.POLL_INITIAL_PERIOD, Constants.POLL_PERIOD_TWENTY_SECONDS);
      overrides.setProperty(ComputeServiceProperties.POLL_MAX_PERIOD, Constants.POLL_PERIOD_TWENTY_SECONDS);

      ComputeServiceContext context = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .overrides(overrides)
            .modules(modules)
            .buildView(ComputeServiceContext.class);
      compute = context.getComputeService();
   }

   private Set<? extends NodeMetadata> createServer() throws RunNodesException, TimeoutException {
      Template template = compute.templateBuilder()
            .locationId(Constants.ZONE)
            .osDescriptionMatches(".*CentOS 6.2.*")
            .minRam(512)
            .build();

      System.out.println("Create Server");

      // This method will continue to poll for the server status and won't return until this server is ACTIVE
      // If you want to know what's happening during the polling, enable logging.
      // See /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      Set<? extends NodeMetadata> nodes = compute.createNodesInGroup(Constants.NAME, numServers, template);

      for (NodeMetadata nodeMetadata: nodes) {
         System.out.println("  " + nodeMetadata);         
      }

      return nodes;
   }

   private void configureAndStartWebserver(Set<? extends NodeMetadata> nodes) throws TimeoutException {
      for (NodeMetadata nodeMetadata: nodes) {
         String publicAddress = nodeMetadata.getPublicAddresses().iterator().next();
         String privateAddress = nodeMetadata.getPrivateAddresses().iterator().next();

         System.out.println("Configure And Start Webserver");

         awaitSsh(publicAddress);

         String message = new StringBuilder()
         .append("Hello from ").append(nodeMetadata.getHostname())
         .append(" @ ").append(publicAddress).append("/").append(privateAddress)
         .append(" in ").append(nodeMetadata.getLocation().getParent().getId())
         .toString();

         String script = new ScriptBuilder().addStatement(exec("yum -y install httpd"))
               .addStatement(exec("/usr/sbin/apachectl start"))
               .addStatement(exec("iptables -I INPUT -p tcp --dport 80 -j ACCEPT"))
               .addStatement(exec("echo '" + message + "' > /var/www/html/index.html"))
               .render(OsFamily.UNIX);

         RunScriptOptions options = RunScriptOptions.Builder.blockOnComplete(true);

         compute.runScriptOnNode(nodeMetadata.getId(), script, options);

         System.out.println("  Login: ssh " + nodeMetadata.getCredentials().getUser() + "@" + publicAddress);
         System.out.println("  Password: " + nodeMetadata.getCredentials().getPassword());
         System.out.println("  Go to http://" + publicAddress);
      }
   }

   private void awaitSsh(String ip) throws TimeoutException {
      SocketOpen socketOpen = compute.getContext().utils().injector().getInstance(SocketOpen.class);
      Predicate<HostAndPort> socketTester = retry(socketOpen, 300, 5, 5, SECONDS);
      socketTester.apply(HostAndPort.fromParts(ip, 22));
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(compute.getContext());
   }
}
