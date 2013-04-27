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
package org.jclouds.examples.rackspace;

import static com.google.common.io.Closeables.closeQuietly;

import java.io.Closeable;
import java.util.Properties;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.openstack.keystone.v2_0.config.CredentialTypes;
import org.jclouds.openstack.keystone.v2_0.config.KeystoneProperties;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.rest.RestContext;

/**
 * To authenticate using jclouds you need to provide your credentials to a Context as in the init() method below. 
 * Authentication occurs on your first actual interaction with the Rackspace Cloud (i.e. the very first time 
 * you call a method that needs to talk to the cloud). Once you are authenticated you receive a token that is
 * cached and you won't reauthenticate for subsequent calls. If your token expires before the JVM quits, jclouds
 * will automatically handle reauthentication and get a new token for you.
 * 
 * If authentication doesn't work, the call will result in an org.jclouds.rest.AuthorizationException
 * 
 * This example demostrates how you would authenticate via username and password or API key. The default is
 * authentication via API key, which is used in the rest of the examples in this package.
 * 
 * @author Everett Toews
 */
public class Authentication implements Closeable {
   private ComputeService compute;
   private RestContext<NovaApi, NovaAsyncApi> nova;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key or password
    * [Optional] The third argument (args[2]) must be "password" if password authentication is used, 
    *            otherwise default to using API key.
    */
   public static void main(String[] args) {
      Authentication authentication = new Authentication();

      try {
         authentication.init(args);
         authentication.authenticateOnCall();
      }
      finally {
         authentication.close();
      }
   }

   private void init(String[] args) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the provider to "rackspace-cloudservers-uk"
      String provider = "rackspace-cloudservers-us";

      String username = args[0];
      String credential = args[1];

      Properties overrides = new Properties();

      if (args.length == 3 && "password".equals(args[2])) {
         overrides.put(KeystoneProperties.CREDENTIAL_TYPE, CredentialTypes.PASSWORD_CREDENTIALS);
      }

      ComputeServiceContext context = ContextBuilder.newBuilder(provider)
            .credentials(username, credential)
            .overrides(overrides)
            .buildView(ComputeServiceContext.class);
      compute = context.getComputeService();
      nova = context.unwrap();
   }

   /**
    * Calling getConfiguredZones() causes jclouds to authenticate. If authentication doesn't work, the call to
    * getConfiguredZones() will result in an org.jclouds.rest.AuthorizationException
    */
   private void authenticateOnCall() {
      System.out.println("Authenticate On Call");

      nova.getApi().getConfiguredZones();

      System.out.println("  Authenticated");
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(compute.getContext());
   }
}
