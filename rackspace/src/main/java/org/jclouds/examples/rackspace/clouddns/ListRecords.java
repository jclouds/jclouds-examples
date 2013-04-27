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
import static org.jclouds.examples.rackspace.clouddns.Constants.NAME;

import java.io.Closeable;

import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;
import org.jclouds.rackspace.clouddns.v1.domain.RecordDetail;

/**
 * This example lists records. 
 *  
 * @author Everett Toews
 */
public class ListRecords implements Closeable {
   private CloudDNSApi dnsApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      ListRecords listRecords = new ListRecords();

      try {
         listRecords.init(args);
         Domain domain = listRecords.getDomain();
         listRecords.listRecords(domain);
         listRecords.listRecordsByNameAndType(domain);
         listRecords.listRecordsByType(domain);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         listRecords.close();
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

   private Domain getDomain() {
      for (Domain domain: dnsApi.getDomainApi().list().concat()) {
         if (domain.getName().startsWith(NAME)) {
            return domain;
         }
      }
      
      throw new RuntimeException(NAME + " not found. Run CreateDomains example first.");
   }

   private void listRecords(Domain domain) {
      System.out.println("List Records");

      Iterable<RecordDetail> recordDetails = dnsApi.getRecordApiForDomain(domain.getId()).list().concat();
      
      for (RecordDetail recordDetail: recordDetails) {
         System.out.println("  " + recordDetail);
      }
   }

   private void listRecordsByNameAndType(Domain domain) {
      System.out.println("List Records by Name and Type");

      Iterable<RecordDetail> recordDetails = dnsApi.getRecordApiForDomain(domain.getId()).listByNameAndType(NAME, "A").concat();
      
      for (RecordDetail recordDetail: recordDetails) {
         System.out.println("  " + recordDetail);
      }
   }

   private void listRecordsByType(Domain domain) {
      System.out.println("List Records by Type");

      Iterable<RecordDetail> recordDetails = dnsApi.getRecordApiForDomain(domain.getId()).listByType("MX").concat();
      
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
