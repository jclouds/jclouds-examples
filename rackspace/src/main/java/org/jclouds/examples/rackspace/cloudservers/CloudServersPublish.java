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

import static org.jclouds.scriptbuilder.domain.Statements.exec;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.config.ComputeServiceProperties;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.predicates.InetSocketAddressConnect;
import org.jclouds.predicates.RetryablePredicate;
import org.jclouds.scriptbuilder.ScriptBuilder;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;
import com.google.inject.Module;

/**
 * This example will create a server, start a webserver on it, and publish a
 * page on the internet!
 */
public class CloudServersPublish {
   private static final String GROUP_NAME = "jclouds-example";
   private static final String LOCATION = "DFW";

   private ComputeService compute;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username The second argument
    * (args[1]) must be your API key
    */
   public static void main(String[] args) {
      CloudServersPublish cloudServersPublish = new CloudServersPublish();

      try {
         cloudServersPublish.init(args);
         NodeMetadata node = cloudServersPublish.createServer();
         cloudServersPublish.configureAndStartWebserver(node);
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         cloudServersPublish.close();
      }
   }

   private void init(String[] args) {
      // The provider configures jclouds to use the Rackspace open cloud (US)
      // to use the Rackspace open cloud (UK) set the provider to
      // "rackspace-cloudservers-uk"
      String provider = "rackspace-cloudservers-us";

      String username = args[0];
      String apiKey = args[1];

      Iterable<Module> modules = ImmutableSet.<Module> of(new SshjSshClientModule());

      // These properties control how often jclouds polls for a status udpate
      Properties overrides = new Properties();
      overrides.setProperty(ComputeServiceProperties.POLL_INITIAL_PERIOD, "20000");
      overrides.setProperty(ComputeServiceProperties.POLL_MAX_PERIOD, "20000");

      ComputeServiceContext context = ContextBuilder.newBuilder(provider).credentials(username, apiKey)
            .modules(modules).buildView(ComputeServiceContext.class);
      compute = context.getComputeService();
   }

   private NodeMetadata createServer() throws RunNodesException, TimeoutException {
      Template template = compute.templateBuilder().locationId(LOCATION).osDescriptionMatches(".*CentOS 6.2.*")
            .minRam(512).build();

      System.out.println("Create Server");

      // This method will continue to poll for the server status and won't
      // return until this server is ACTIVE
      // If you want to know what's happening during the polling, enable
      // logging. See
      // /jclouds-exmaple/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      Set<? extends NodeMetadata> nodes = compute.createNodesInGroup(GROUP_NAME, 1, template);

      NodeMetadata nodeMetadata = nodes.iterator().next();

      System.out.println("  " + nodeMetadata);

      return nodeMetadata;
   }

   private void configureAndStartWebserver(NodeMetadata node) throws TimeoutException {
      String publicAddress = node.getPublicAddresses().iterator().next();

      System.out.println("Configure And Start Webserver");

      waitForSsh(publicAddress);

      String script = new ScriptBuilder()
            .addStatement(exec("yum -y install httpd"))
            .addStatement(exec("/usr/sbin/apachectl start"))
            .addStatement(exec("iptables -I INPUT -p tcp --dport 80 -j ACCEPT"))
            .addStatement(exec("echo 'Hello Cloud Servers' > /var/www/html/index.html")).render(OsFamily.UNIX);

      RunScriptOptions options = RunScriptOptions.Builder.blockOnComplete(true);

      compute.runScriptOnNode(node.getId(), script, options);

      System.out.println("  Login: ssh " + node.getCredentials().getUser() + "@" + publicAddress);
      System.out.println("  Password: " + node.getCredentials().getPassword());
      System.out.println("  Go to http://" + publicAddress);
   }

   private void waitForSsh(String ip) throws TimeoutException {
      RetryablePredicate<HostAndPort> blockUntilSSHReady = new RetryablePredicate<HostAndPort>(
            new InetSocketAddressConnect(), 300, 5, 5, TimeUnit.SECONDS);

      if (!blockUntilSSHReady.apply(HostAndPort.fromParts(ip, 22)))
         throw new TimeoutException("Timeout on ssh: " + ip);
   }

   /**
    * Always close your service when you're done with it.
    */
   private void close() {
      if (compute != null) {
         compute.getContext().close();
      }
   }
}
