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

package org.jclouds.examples.minecraft;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.get;
import static org.jclouds.compute.options.TemplateOptions.Builder.runAsRoot;

import java.io.File;
import java.io.IOException;

import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.domain.LoginCredentials;

import com.google.common.base.Function;
import com.google.common.io.Files;
import com.google.common.net.HostAndPort;

/**
 * 
 * @author Adrian Cole
 */
public class Utils {
   public static Function<ExecResponse, String> getStdout() {
      return new Function<ExecResponse, String>() {

         @Override
         public String apply(ExecResponse input) {
            return input.getOutput();
         }
      };
   }

   public static Function<NodeMetadata, HostAndPort> firstPublicAddressToHostAndPort(final int port) {
      return new Function<NodeMetadata, HostAndPort>() {

         @Override
         public HostAndPort apply(NodeMetadata input) {
            return HostAndPort.fromParts(get(input.getPublicAddresses(), 0), port);
         }

         @Override
         public String toString() {
            return "firstPublicAddressToHostAndPort(" + port + ")";
         }

      };
   }

   public static TemplateOptions asCurrentUser() {
      return runAsRoot(false).overrideLoginCredentials(currentUser());
   }

   public static LoginCredentials currentUser() {
      String privateKeyKeyFile = System.getProperty("user.home") + "/.ssh/id_rsa";
      String privateKey;
      try {
         privateKey = Files.toString(new File(privateKeyKeyFile), UTF_8);
      } catch (IOException e) {
         throw propagate(e);
      }
      assert privateKey.startsWith("-----BEGIN RSA PRIVATE KEY-----") : "invalid key:\n" + privateKey;
      return LoginCredentials.builder().user(System.getProperty("user.name")).privateKey(privateKey).build();
   }

}
