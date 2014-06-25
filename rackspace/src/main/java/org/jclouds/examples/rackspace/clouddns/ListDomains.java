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

import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;
import org.jclouds.rackspace.clouddns.v1.domain.Subdomain;

import java.io.Closeable;
import java.io.IOException;

import static org.jclouds.examples.rackspace.clouddns.Constants.*;

/**
 * This example lists domains.
 *
 */
public class ListDomains implements Closeable {
   private final CloudDNSApi dnsApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      ListDomains listDomains = new ListDomains(args[0], args[1]);

      try {
         int domainId = listDomains.listDomains();
         listDomains.listWithFilterByNamesMatching();
         listDomains.listSubdomains(domainId);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         listDomains.close();
      }
   }

   public ListDomains(String username, String apiKey) {
      dnsApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CloudDNSApi.class);
   }

   private int listDomains() {
      System.out.format("List Domains%n");

      Iterable<Domain> domains = dnsApi.getDomainApi().list().concat();
      int domainId = 0;

      for (Domain domain: domains) {
         System.out.format("  %s%n", domain);

         if (domain.getName().equals(NAME)) {
            domainId = domain.getId();
         }
      }

      return domainId;
   }

   private void listWithFilterByNamesMatching() {
      System.out.format("List With Filter By Names Matching%n");

      Iterable<Domain> domains = dnsApi.getDomainApi().listWithFilterByNamesMatching(ALT_NAME).concat();

      for (Domain domain: domains) {
         System.out.format("  %s%n", domain);
      }
   }

   private void listSubdomains(int domainId) {
      System.out.format("List Subdomains%n");

      Iterable<Subdomain> subdomains = dnsApi.getDomainApi().listSubdomains(domainId).concat();

      for (Subdomain subdomain: subdomains) {
         System.out.format("  %s%n", subdomain);
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
