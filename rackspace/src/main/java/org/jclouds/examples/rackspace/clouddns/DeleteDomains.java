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
import static org.jclouds.examples.rackspace.clouddns.Constants.GET_DOMAIN_ID;
import static org.jclouds.examples.rackspace.clouddns.Constants.IS_DOMAIN;
import static org.jclouds.rackspace.clouddns.v1.predicates.JobPredicates.awaitComplete;

import java.io.Closeable;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;

import com.google.common.collect.Iterables;

/**
 * This example deletes all domains. 
 *  
 * @author Everett Toews
 */
public class DeleteDomains implements Closeable {
   private CloudDNSApi dnsApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      DeleteDomains deleteDomains = new DeleteDomains();

      try {
         deleteDomains.init(args);
         deleteDomains.deleteAllDomains();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         deleteDomains.close();
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

   private void deleteAllDomains() throws TimeoutException {
      System.out.println("Delete Domains");
      
      Set<Domain> allDomains = dnsApi.getDomainApi().list().concat().toImmutableSet();
      Iterable<Domain> topLevelDomains = Iterables.filter(allDomains, IS_DOMAIN);
      Iterable<Integer> topLevelDomainIds = Iterables.transform(topLevelDomains, GET_DOMAIN_ID);
      
      awaitComplete(dnsApi, dnsApi.getDomainApi().delete(topLevelDomainIds, true));
            
      System.out.println("  Deleted " + Iterables.size(topLevelDomainIds) + " top level domains and all subdomains");
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(dnsApi);
   }
}
