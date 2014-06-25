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
package org.jclouds.examples.rackspace;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
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
 */
public class Logging implements Closeable {
   private final NovaApi nova;

    /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      Logging logging = new Logging(args[0], args[1]);

      try {
         logging.getConfiguredZones();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         logging.close();
      }
   }

   public Logging(String username, String apiKey) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the system property or default value to "rackspace-cloudservers-uk"
      String provider = System.getProperty("provider.cs", "rackspace-cloudservers-us");

      // This module is responsible for enabling logging
      Iterable<Module> modules = ImmutableSet.<Module> of(new SLF4JLoggingModule());

      nova = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .modules(modules) // don't forget to add the modules to your context!
            .buildApi(NovaApi.class);
   }

   private void getConfiguredZones() {
       // Calling getConfiguredZones() talks to the cloud which gets logged
       Set<String> zones = nova.getConfiguredZones();

       System.out.format("Zones%n");

       for (String zone : zones) {
           System.out.format("  %s%n", zone);
       }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(nova, true);
   }
}
