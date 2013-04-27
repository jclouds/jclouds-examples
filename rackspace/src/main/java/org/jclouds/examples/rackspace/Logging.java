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
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.rest.RestContext;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

/**
 * This example shows you how to log what jclouds is doing. This is extremely useful for debugging, submitting bug
 * reports, getting help, and figuring exactly what the HTTP requests and resonses look like.
 * 
 * In this example we use the Simple Logging Facade for Java (SLF4J). The implementation of SLF4J that we'll use is
 * Logback so you'll need to download the Logback JARs from http://logback.qos.ch/download.html and put them on your
 * classpath.
 * 
 * The last key ingredient is the file /jclouds-examples/rackspace/src/main/resources/logback.xml which configures
 * the logging. As it is configured right now it will log wire input/output and headers to standard out (STDOUT).
 * This means that you will be able to see on your console everything that is sent in the request (marked by ">>")
 * and everything received in the response (marked by "<<").
 * 
 * @author Everett Toews
 */
public class Logging implements Closeable {
   private ComputeService compute;
   private RestContext<NovaApi, NovaAsyncApi> nova;
   private Set<String> zones;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      Logging logging = new Logging();

      try {
         logging.init(args);
      }
      finally {
         logging.close();
      }
   }

   private void init(String[] args) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the provider to "rackspace-cloudservers-uk"
      String provider = "rackspace-cloudservers-us";

      String username = args[0];
      String apiKey = args[1];

      // This module is responsible for enabling logging
      Iterable<Module> modules = ImmutableSet.<Module> of(new SLF4JLoggingModule());

      ComputeServiceContext context = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .modules(modules) // don't forget to add the modules to your context!
            .buildView(ComputeServiceContext.class);
      compute = context.getComputeService();
      nova = context.unwrap();

      // Calling getConfiguredZones() talks to the cloud which gets logged
      zones = nova.getApi().getConfiguredZones();
      System.out.println("Zones: " + zones);
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(compute.getContext());
   }
}
