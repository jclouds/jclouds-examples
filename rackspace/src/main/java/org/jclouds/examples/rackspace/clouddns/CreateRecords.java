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
import static org.jclouds.rackspace.clouddns.v1.predicates.JobPredicates.awaitComplete;

import java.io.Closeable;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;
import org.jclouds.rackspace.clouddns.v1.domain.Record;
import org.jclouds.rackspace.clouddns.v1.domain.RecordDetail;

import com.google.common.collect.ImmutableSet;

/**
 * This example creates records on an existing domain. 
 *  
 * @author Everett Toews
 */
public class CreateRecords implements Closeable {
   private CloudDNSApi dnsApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      CreateRecords createRecords = new CreateRecords();

      try {
         createRecords.init(args);
         createRecords.createRecords();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         createRecords.close();
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

   private void createRecords() throws TimeoutException {
      System.out.println("Create Records");
      
      int domainId = 0;

      Iterable<Domain> domains = dnsApi.getDomainApi().list().concat();
      
      for (Domain domain: domains) {
         if (domain.getName().equals(ALT_NAME)) {
            domainId = domain.getId();
         }
      }

      Record createTXTRecord = Record.builder()
            .name(ALT_NAME)
            .type("TXT")
            .data("This is a TXT record")
            .build();
      
      Record createARecord = Record.builder()
            .name(ALT_NAME)
            .type("A")
            .data("10.0.0.2")
            .build();
      
      Set<Record> createRecords = ImmutableSet.of(createTXTRecord, createARecord);
      
      Set<RecordDetail> recordDetails = awaitComplete(dnsApi, dnsApi.getRecordApiForDomain(domainId).create(createRecords));

      for (RecordDetail recordDetail: recordDetails) {
         System.out.println("  " + recordDetail);         
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(dnsApi);
   }
}
