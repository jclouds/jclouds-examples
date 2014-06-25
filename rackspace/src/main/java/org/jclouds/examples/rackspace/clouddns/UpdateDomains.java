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
package org.jclouds.examples.rackspace.clouddns;

import com.google.common.collect.Iterables;
import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;
import org.jclouds.rackspace.clouddns.v1.domain.UpdateDomain;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.jclouds.examples.rackspace.clouddns.Constants.*;
import static org.jclouds.rackspace.clouddns.v1.predicates.JobPredicates.awaitComplete;

/**
 * This example updates a domain.
 *
 */
public class UpdateDomains implements Closeable {
   private final CloudDNSApi dnsApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      UpdateDomains updateDomains = new UpdateDomains(args[0], args[1]);

      try {
         updateDomains.updateDomain();
         updateDomains.updateDomains();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         updateDomains.close();
      }
   }

   public UpdateDomains(String username, String apiKey) {
      dnsApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CloudDNSApi.class);
   }

   private void updateDomain() throws TimeoutException {
      System.out.format("Update Domain%n");

      int domainId = 0;

      Iterable<Domain> domains = dnsApi.getDomainApi().list().concat();

      for (Domain domain: domains) {
         if (domain.getName().equals(ALT_NAME)) {
            domainId = domain.getId();
         }
      }

      UpdateDomain updateDomain = UpdateDomain.builder()
            .email("jcloudie@" + ALT_NAME)
            .ttl(600001)
            .comment("Hello Domain Update")
            .build();

      awaitComplete(dnsApi, dnsApi.getDomainApi().update(domainId, updateDomain));

      System.out.format("  %s%n", dnsApi.getDomainApi().get(domainId));
   }

   private void updateDomains() throws TimeoutException {
      System.out.format("Update Domains%n");

      Iterable<Domain> domains = dnsApi.getDomainApi().list().concat();
      Iterable<Integer> domainIds = Iterables.transform(domains, GET_DOMAIN_ID);

      awaitComplete(dnsApi, dnsApi.getDomainApi().updateEmail(domainIds, "jclouder@" + NAME));

      domains = dnsApi.getDomainApi().list().concat();

      for (Domain domain: domains) {
         System.out.format("  %s%n", domain);
      }
   }

   /**
    * Always close your service when you're done with it.
    *
    * Note that closing quietly like this is not necessary in Java 7.
    * You would use try-with-resources in the main method instead.
    */
   public void close() throws IOException {
      Closeables.close(dnsApi, true);
   }
}
