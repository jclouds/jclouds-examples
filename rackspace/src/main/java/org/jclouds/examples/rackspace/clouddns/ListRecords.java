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
import org.jclouds.rackspace.clouddns.v1.domain.RecordDetail;

import java.io.Closeable;
import java.io.IOException;

import static org.jclouds.examples.rackspace.clouddns.Constants.NAME;
import static org.jclouds.examples.rackspace.clouddns.Constants.PROVIDER;

/**
 * This example lists records.
 *
 */
public class ListRecords implements Closeable {
   private final CloudDNSApi dnsApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      ListRecords listRecords = new ListRecords(args[0], args[1]);

      try {
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

   public ListRecords(String username, String apiKey) {
      dnsApi = ContextBuilder.newBuilder(PROVIDER)
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
      System.out.format("List Records%n");

      Iterable<RecordDetail> recordDetails = dnsApi.getRecordApiForDomain(domain.getId()).list().concat();

      for (RecordDetail recordDetail: recordDetails) {
         System.out.format("  %s%n", recordDetail);
      }
   }

   private void listRecordsByNameAndType(Domain domain) {
      System.out.format("List Records by Name and Type%n");

      Iterable<RecordDetail> recordDetails = dnsApi.getRecordApiForDomain(domain.getId()).listByNameAndType(NAME, "A").concat();

      for (RecordDetail recordDetail: recordDetails) {
         System.out.format("  %s%n", recordDetail);
      }
   }

   private void listRecordsByType(Domain domain) {
      System.out.format("List Records by Type%n");

      Iterable<RecordDetail> recordDetails = dnsApi.getRecordApiForDomain(domain.getId()).listByType("MX").concat();

      for (RecordDetail recordDetail: recordDetails) {
         System.out.format("  %s%n", recordDetail);
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
