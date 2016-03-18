package org.jclouds.examples.rackspace.carina;/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.docker.DockerApi;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.google.inject.Module;

public class Utils {
   public static DockerApi getDockerApiFromCarinaDirectory(String path) throws IOException {
      // docker.ps1 contains the endpoint
      String endpoint = "https://" +
            Files.readFirstLine(new File(joinPath(path, "docker.ps1")),
                  Charset.forName("UTF-8")).split("=")[1].replace("\"", "").substring(6);

      // enable logging
      Iterable<Module> modules = ImmutableSet.<Module> of(new SLF4JLoggingModule());
      Properties overrides = new Properties();

      // disable certificate checking for Carina
      overrides.setProperty("jclouds.trust-all-certs", "true");

      return ContextBuilder.newBuilder("docker")
            // Use the unencrypted credentials
            .credentials(joinPath(path, "cert.pem"), joinPath(path, "key.pem"))
            .overrides(overrides)
            .endpoint(endpoint)
            .modules(modules)
            .buildApi(DockerApi.class);
   }

   public static ComputeServiceContext getComputeApiFromCarinaDirectory(String path) throws IOException {
      // docker.ps1 contains the endpoint
      String endpoint = "https://" +
            Files.readFirstLine(new File(joinPath(path, "docker.ps1")),
                  Charset.forName("UTF-8")).split("=")[1].replace("\"", "").substring(6);

      // enable logging and sshj
      Iterable<Module> modules = ImmutableSet.<Module> of(new SLF4JLoggingModule(), new SshjSshClientModule());
      Properties overrides = new Properties();

      // disable certificate checking for Carina
      overrides.setProperty("jclouds.trust-all-certs", "true");

      return ContextBuilder.newBuilder("docker")
            .credentials(joinPath(path, "cert.pem"), joinPath(path, "key.pem"))
            .modules(modules)
            .overrides(overrides)
            .endpoint(endpoint)
            .buildView(ComputeServiceContext.class);
   }

   // Concatenate two different paths
   public static String joinPath(String path1, String path2) {
      return new File(path1, path2).toString();
   }
}
