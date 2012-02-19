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

package org.jclouds.examples.minecraft;

import static java.lang.String.format;
import static org.jclouds.scriptbuilder.domain.Statements.exec;
import static org.jclouds.scriptbuilder.domain.Statements.saveHttpResponseTo;

import java.net.URI;

import javax.inject.Named;

import org.jclouds.scriptbuilder.InitScript;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/**
 * 
 * @author Adrian Cole
 */
public class ConfigureMinecraftDaemon extends AbstractModule {
   
   @Override
   protected void configure() {
      
   }

   @Provides
   InitScript configureMinecraftDaemon(@Named("minecraft.url") String url, @Named("minecraft.ms") int minHeap,
         @Named("minecraft.mx") int maxHeap) {
      return InitScript.builder().name("minecraft")
            .init(saveHttpResponseTo(URI.create(url), "${INSTANCE_HOME}", "minecraft_server.jar"))
            .run(exec(format("java -Xms%sm -Xmx%sm -jar minecraft_server.jar", minHeap, maxHeap))).build();
   }


}