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
import static org.jclouds.examples.rackspace.clouddns.Constants.CLOUD_SERVERS;
import static org.jclouds.examples.rackspace.clouddns.Constants.NAME;
import static org.jclouds.rackspace.clouddns.v1.predicates.JobPredicates.awaitComplete;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.examples.rackspace.cloudservers.CloudServersPublish;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Record;
import org.jclouds.rackspace.clouddns.v1.domain.RecordDetail;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * This example creates reverse DNS records on an existing domain for a Cloud Server. 
 *  
 * @author Everett Toews
 */
public class CRUDReverseDNSRecords implements Closeable {
   private static NodeMetadata node;
   private static RecordDetail recordDetail;

   private CloudDNSApi dnsApi;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      CRUDReverseDNSRecords crudReverseDNSRecords = new CRUDReverseDNSRecords();

      try {
         List<String> argsList = Lists.newArrayList(args);
         argsList.add("1"); // the number of Cloud Servers to start
         node = CloudServersPublish.getPublishedCloudServers(argsList).iterator().next();
         
         crudReverseDNSRecords.init(args);
         crudReverseDNSRecords.createReverseDNSRecords();
         crudReverseDNSRecords.listReverseDNSRecords();
         crudReverseDNSRecords.updateReverseDNSRecords();
         crudReverseDNSRecords.deleteAllReverseDNSRecords();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         crudReverseDNSRecords.close();
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

   private void createReverseDNSRecords() throws TimeoutException {
      System.out.println("Create Reverse DNS Records");
      
      Record createPTRRecordIPv4 = Record.builder()
            .name(NAME)
            .type("PTR")
            .data(node.getPublicAddresses().iterator().next())
            .ttl(11235)
            .build();

      Set<Record> records = ImmutableSet.<Record> of(createPTRRecordIPv4);

      Iterable<RecordDetail> recordDetails = awaitComplete(dnsApi, dnsApi.getReverseDNSApiForService(CLOUD_SERVERS).create(node.getUri(), records));
      
      recordDetail = recordDetails.iterator().next();
      System.out.println("  " + recordDetail);
   }

   private void listReverseDNSRecords() {
      System.out.println("List Reverse DNS Records");

      Iterable<RecordDetail> recordDetails = dnsApi.getReverseDNSApiForService(CLOUD_SERVERS).list(node.getUri()).concat();
      
      for (RecordDetail recordDetail: recordDetails) {
         System.out.println("  " + recordDetail);
      }
   }

   private void updateReverseDNSRecords() throws TimeoutException {
      System.out.println("Update Reverse DNS Records");

      Record updatePTRRecord = recordDetail.getRecord().toBuilder().comment("Hello Cloud DNS").build();
      Map<String, Record> idsToRecords = ImmutableMap.<String, Record> of(recordDetail.getId(), updatePTRRecord);

      awaitComplete(dnsApi, dnsApi.getReverseDNSApiForService(CLOUD_SERVERS).update(node.getUri(), idsToRecords));
      
      System.out.println("  " + dnsApi.getReverseDNSApiForService(CLOUD_SERVERS).get(node.getUri(), recordDetail.getId()));
   }

   private void deleteAllReverseDNSRecords() throws TimeoutException {
      System.out.println("Delete Reverse DNS Records");

      awaitComplete(dnsApi, dnsApi.getReverseDNSApiForService(CLOUD_SERVERS).deleteAll(node.getUri()));
      
      System.out.println("  Deleted all reverse DNS records");
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(dnsApi);
   }
}
