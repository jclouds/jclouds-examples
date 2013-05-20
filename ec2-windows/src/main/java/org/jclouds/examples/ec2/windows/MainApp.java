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

package org.jclouds.examples.ec2.windows;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.inject.Module;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.ec2.reference.AWSEC2Constants;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.domain.Location;
import org.jclouds.encryption.bouncycastle.config.BouncyCastleCryptoModule;
import org.jclouds.location.predicates.LocationPredicates;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import javax.annotation.Nullable;
import java.util.Properties;
import java.util.Set;

/**
 * The main application. This will parse and validate the command line
 * arguments, initialize a jclouds context, and then jump to {@link
 * WindowsInstanceStarter}.
 *
 * @author Richard Downer
 */
public class MainApp {

   private ComputeServiceContext context;
   private ComputeService computeService;
   private Arguments arguments;

   public MainApp(Arguments arguments) {
      this.arguments = arguments;
   }

   public static void main(String[] args) throws Exception {
      Arguments arguments = new Arguments();
      CmdLineParser parser = new CmdLineParser(arguments);
      try {
         parser.parseArgument(args);
      } catch (CmdLineException e) {
         // handling of wrong arguments
         System.err.println(e.getMessage());
         parser.printUsage(System.err);
         System.exit(1);
      }
      new MainApp(arguments).run();
   }

   private void run() throws Exception {
      Properties overrides = new Properties();
      overrides.put(AWSEC2Constants.PROPERTY_EC2_AMI_QUERY, "owner-id=" + arguments.getAmiOwner() + ";state=available;image-type=machine");

      ImmutableSet<Module> modules = ImmutableSet.<Module>of(
         new SLF4JLoggingModule(), // OverThere uses SLF4J so we will as well
         new BouncyCastleCryptoModule() // needed to decrypt the password from EC2
      );
      context = ContextBuilder.newBuilder("aws-ec2")
         .credentials(arguments.getIdentity(), arguments.getCredential())
         .overrides(overrides)
         .modules(modules)
         .build(ComputeServiceContext.class);

      try {
         computeService = context.getComputeService();
         Set<String> regions = Sets.newHashSet(Iterables.transform(Iterables.filter(computeService.listAssignableLocations(), LocationPredicates.isRegion()), new Function<Location, String>() {
            @Override
            public String apply(@Nullable Location location) {
               return (location != null) ? location.getId() : null;
            }
         }));

         if (!regions.contains(arguments.getRegion())) {
            System.err.println("Region \"" + arguments.getRegion() + "\" is not known. Known regions are:");
            for (String r : regions) {
               System.err.println("    " + r);
            }
            System.exit(1);
         }

         WindowsInstanceStarter app = new WindowsInstanceStarter(arguments, context);
         app.run();
      } finally {
         context.close();
      }
   }

}
