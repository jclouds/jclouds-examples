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

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;
import org.jclouds.rackspace.clouddns.v1.domain.Record;
import org.jclouds.rackspace.clouddns.v1.domain.RecordDetail;
import org.jclouds.rackspace.clouddns.v1.functions.RecordFunctions;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static org.jclouds.examples.rackspace.clouddns.Constants.NAME;
import static org.jclouds.examples.rackspace.clouddns.Constants.PROVIDER;
import static org.jclouds.rackspace.clouddns.v1.predicates.JobPredicates.awaitComplete;

/**
 * This example updates the records on a domain.
 *
 */
public class UpdateRecords implements Closeable {
   private final CloudDNSApi dnsApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      UpdateRecords updateRecords = new UpdateRecords(args[0], args[1]);

      try {
         Domain domain = updateRecords.getDomain();
         updateRecords.updateRecord(domain);
         updateRecords.updateRecords(domain);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         updateRecords.close();
      }
   }

   public UpdateRecords(String username, String apiKey) {
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

   private void updateRecord(Domain domain) throws TimeoutException {
      System.out.format("Update Record%n");

      RecordDetail recordDetail = dnsApi.getRecordApiForDomain(domain.getId()).getByNameAndTypeAndData(NAME, "A", "10.0.0.1");
      Record updateRecord = recordDetail.getRecord().toBuilder().data("10.0.1.0").build();

      awaitComplete(dnsApi, dnsApi.getRecordApiForDomain(domain.getId()).update(recordDetail.getId(), updateRecord));

      System.out.format("  %s%n", dnsApi.getRecordApiForDomain(domain.getId()).get(recordDetail.getId()));
   }

   private void updateRecords(Domain domain) throws TimeoutException {
      System.out.format("Update Records%n");

      Set<RecordDetail> recordDetails = dnsApi.getRecordApiForDomain(domain.getId()).listByType("A").concat().toSet();
      Map<String, Record> idsToRecords = RecordFunctions.toRecordMap(recordDetails);
      Map<String, Record> updateRecords = Maps.transformValues(idsToRecords, updateTTLAndComment(235813, "New TTL"));

      awaitComplete(dnsApi, dnsApi.getRecordApiForDomain(domain.getId()).update(updateRecords));

      Iterable<RecordDetail> recordDetailsUpdated = dnsApi.getRecordApiForDomain(domain.getId()).listByType("A").concat();

      for (RecordDetail recordDetailUpdated: recordDetailsUpdated) {
         System.out.format("  %s%n", recordDetailUpdated);
      }
   }

   private Function<Record, Record> updateTTLAndComment(final int ttl, final String comment) {
      return new Function<Record, Record>() {
         public Record apply(Record record) {
            return record.toBuilder().ttl(ttl).comment(comment).build();
         }
      };
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
