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

import java.io.Closeable;

import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;
import org.jclouds.rackspace.clouddns.v1.domain.Subdomain;

/**
 * This example lists domains. 
 *  
 * @author Everett Toews
 */
public class ListDomains implements Closeable {
   private CloudDNSApi dnsApi;
   private int domainId;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      ListDomains listDomains = new ListDomains();

      try {
         listDomains.init(args);
         listDomains.listDomains();
         listDomains.listWithFilterByNamesMatching();
         listDomains.listSubdomains();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         listDomains.close();
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

   private void listDomains() {
      System.out.println("List Domains");

      Iterable<Domain> domains = dnsApi.getDomainApi().list().concat();
      
      for (Domain domain: domains) {
         System.out.println("  " + domain);
         
         if (domain.getName().equals(NAME)) {
            domainId = domain.getId();
         }
      }
   }

   private void listWithFilterByNamesMatching() {
      System.out.println("List With Filter By Names Matching");
      
      Iterable<Domain> domains = dnsApi.getDomainApi().listWithFilterByNamesMatching(ALT_NAME).concat();
      
      for (Domain domain: domains) {
         System.out.println("  " + domain);         
      }
   }

   private void listSubdomains() {
      System.out.println("List Subdomains");

      Iterable<Subdomain> subdomains = dnsApi.getDomainApi().listSubdomains(domainId).concat();
      
      for (Subdomain subdomain: subdomains) {
         System.out.println("  " + subdomain);         
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(dnsApi);
   }
}
