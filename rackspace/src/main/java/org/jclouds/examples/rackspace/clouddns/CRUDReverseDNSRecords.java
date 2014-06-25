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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.examples.rackspace.cloudservers.CloudServersPublish;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Record;
import org.jclouds.rackspace.clouddns.v1.domain.RecordDetail;
import org.jclouds.rackspace.clouddns.v1.features.ReverseDNSApi;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static org.jclouds.examples.rackspace.clouddns.Constants.*;
import static org.jclouds.rackspace.clouddns.v1.predicates.JobPredicates.awaitComplete;

/**
 * This example creates reverse DNS records on an existing domain for a Cloud Server.
 *
 */
public class CRUDReverseDNSRecords implements Closeable {
   private final CloudDNSApi dnsApi;
   private final ReverseDNSApi reverseDNSApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      CRUDReverseDNSRecords crudReverseDNSRecords = new CRUDReverseDNSRecords(args[0], args[1]);

      try {
         List<String> argsList = Lists.newArrayList(args);
         argsList.add("1"); // the number of Cloud Servers to start
         NodeMetadata node = CloudServersPublish.getPublishedCloudServers(argsList).iterator().next();

         RecordDetail recordDetail = crudReverseDNSRecords.createReverseDNSRecords(node);
         crudReverseDNSRecords.listReverseDNSRecords(node);
         crudReverseDNSRecords.updateReverseDNSRecords(node, recordDetail);
         crudReverseDNSRecords.deleteAllReverseDNSRecords(node);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         crudReverseDNSRecords.close();
      }
   }

   public CRUDReverseDNSRecords(String username, String apiKey) {
      dnsApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CloudDNSApi.class);
      reverseDNSApi = dnsApi.getReverseDNSApiForService(CLOUD_SERVERS);
   }

   private RecordDetail createReverseDNSRecords(NodeMetadata node) throws TimeoutException {
      System.out.format("Create Reverse DNS Records%n");

      Record createPTRRecordIPv4 = Record.builder()
            .name(NAME)
            .type("PTR")
            .data(node.getPublicAddresses().iterator().next())
            .ttl(11235)
            .build();

      Set<Record> records = ImmutableSet.of(createPTRRecordIPv4);

      Iterable<RecordDetail> recordDetails = awaitComplete(dnsApi, reverseDNSApi.create(node.getUri(), records));

      RecordDetail recordDetail = recordDetails.iterator().next();
      System.out.format("  %s%n", recordDetail);

      return recordDetail;
   }

   private void listReverseDNSRecords(NodeMetadata node) {
      System.out.format("List Reverse DNS Records%n");

      Iterable<RecordDetail> recordDetails = reverseDNSApi.list(node.getUri()).concat();

      for (RecordDetail recordDetail: recordDetails) {
         System.out.format("  %s%n", recordDetail);
      }
   }

   private void updateReverseDNSRecords(NodeMetadata node, RecordDetail recordDetail) throws TimeoutException {
      System.out.format("Update Reverse DNS Records%n");

      Record updatePTRRecord = recordDetail.getRecord().toBuilder().comment("Hello Cloud DNS").build();
      Map<String, Record> idsToRecords = ImmutableMap.of(recordDetail.getId(), updatePTRRecord);

      awaitComplete(dnsApi, reverseDNSApi.update(node.getUri(), idsToRecords));

      System.out.format("  %s%n", reverseDNSApi.get(node.getUri(), recordDetail.getId()));
   }

   private void deleteAllReverseDNSRecords(NodeMetadata node) throws TimeoutException {
      System.out.format("Delete Reverse DNS Records%n");

      awaitComplete(dnsApi, dnsApi.getReverseDNSApiForService(CLOUD_SERVERS).deleteAll(node.getUri()));

      System.out.format("  Deleted all reverse DNS records%n");
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
