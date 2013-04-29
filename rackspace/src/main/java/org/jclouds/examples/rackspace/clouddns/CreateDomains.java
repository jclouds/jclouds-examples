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
package org.jclouds.examples.rackspace.clouddns;

import static com.google.common.io.Closeables.closeQuietly;
import static org.jclouds.examples.rackspace.clouddns.Constants.ALT_NAME;
import static org.jclouds.examples.rackspace.clouddns.Constants.NAME;
import static org.jclouds.rackspace.clouddns.v1.predicates.JobPredicates.awaitComplete;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.CreateDomain;
import org.jclouds.rackspace.clouddns.v1.domain.CreateSubdomain;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;
import org.jclouds.rackspace.clouddns.v1.domain.Record;
import org.jclouds.rackspace.clouddns.v1.functions.DomainFunctions;

import com.google.common.collect.ImmutableSet;

/**
 * This example creates a domain with subdomains and records. 
 *  
 * @author Everett Toews
 */
public class CreateDomains implements Closeable {
   private CloudDNSApi dnsApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      CreateDomains createDomains = new CreateDomains();

      try {
         createDomains.init(args);
         createDomains.createDomains();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createDomains.close();
      }
   }

   private void init(String[] args) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the provider to "rackspace-clouddns-uk"
      String provider = "rackspace-clouddns-us";

      String username = args[0];
      String apiKey = args[1];

      dnsApi = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .buildApi(CloudDNSApi.class);
   }

   private void createDomains() throws TimeoutException {
      System.out.println("Create Domains");
      
      Record createMXRecord = Record.builder()
            .name(NAME)
            .type("MX")
            .data("mail." + NAME)
            .priority(11235)
            .build();
      
      Record createARecord = Record.builder()
            .name(NAME)
            .type("A")
            .data("10.0.0.1")
            .build();
      
      Set<Record> createRecords = ImmutableSet.of(createMXRecord, createARecord);
      
      CreateSubdomain createSubdomain1 = CreateSubdomain.builder()
            .name("dev." + NAME)
            .email("jclouds@" + NAME)
            .comment("Hello dev subdomain")
            .build();
      
      CreateSubdomain createSubdomain2 = CreateSubdomain.builder()
            .name("test." + NAME)
            .email("jclouds@" + NAME)
            .comment("Hello test subdomain")
            .build();
      
      Set<CreateSubdomain> createSubdomains = ImmutableSet.of(createSubdomain1, createSubdomain2);

      CreateDomain createDomain1 = CreateDomain.builder()
            .name(NAME)
            .email("jclouds@" + NAME)
            .ttl(600000)
            .comment("Hello Domain")
            .subdomains(createSubdomains)
            .records(createRecords)
            .build();

      CreateDomain createDomain2 = CreateDomain.builder()
            .name(ALT_NAME)
            .email("jclouds@" + ALT_NAME)
            .ttl(600000)
            .comment("Hello Domain")
            .build();

      Set<CreateDomain> createDomains = ImmutableSet.of(createDomain1, createDomain2);
      Map<String, Domain> domains = DomainFunctions.toDomainMap(awaitComplete(dnsApi, dnsApi.getDomainApi().create(createDomains)));
      
      System.out.println("  " + domains.get(NAME));
      System.out.println("  " + domains.get(ALT_NAME));
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(dnsApi);
   }
}
