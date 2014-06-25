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
package org.jclouds.examples.rackspace.clouddatabases;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.openstack.trove.v1.TroveApi;
import org.jclouds.openstack.trove.v1.domain.Instance;
import org.jclouds.openstack.trove.v1.features.InstanceApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.CloudLoadBalancersApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.AddNode;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.CreateLoadBalancer;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.LoadBalancer;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.Node;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.VirtualIP;
import org.jclouds.rackspace.cloudloadbalancers.v1.domain.VirtualIPWithId;
import org.jclouds.rackspace.cloudloadbalancers.v1.features.LoadBalancerApi;
import org.jclouds.rackspace.cloudloadbalancers.v1.predicates.LoadBalancerPredicates;

import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.common.util.concurrent.Uninterruptibles;

import static org.jclouds.examples.rackspace.clouddatabases.Constants.*;

/**
 * This example uses the already created database instance, database user, and database from the examples:
 * CreateInstance, CreateDatabase, CreateUser
 * This example will create a load balancer to allow public access to the database.
 * The load balancer is only needed for public access - it is not needed when accessing the database from the rackspace network.
 * For more information: http://www.rackspace.com/knowledge_center/article/public-vs-private-access
 * The example connects to the database using JDBC over the load balancer and executes a simple command to confirm that the database is online.
 */
public class TestDatabase implements Closeable {
   // If you want to log instead of print, uncomment the line below
   // private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestDatabase.class);
   private final CloudLoadBalancersApi clbApi;
   private final LoadBalancerApi lbApi;
   private final TroveApi troveApi;
   private final InstanceApi instanceApi;

   /**
    * To get a username and API key see
    * http://www.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument  (args[0]) must be your username.
    * The second argument (args[1]) must be your API key.
    */
   public static void main(String[] args) throws IOException {
      TestDatabase testDatabase = new TestDatabase(args[0], args[1]);

      try {
         Set<AddNode> addNodes = testDatabase.addNodesOfDatabaseInstances();
         testDatabase.createLoadBalancer(addNodes);

         boolean success;
         do{
            success = testDatabase.testDatabase();
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
         } while(!success);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         testDatabase.close();
      }
   }

   public TestDatabase(String username, String apiKey) {
      // The provider configures jclouds to use the Rackspace Cloud (US).
      // To use the Rackspace Cloud (UK) set the provider to "rackspace-cloudloadbalancers-uk".
      String provider = System.getProperty("provider.clb", "rackspace-cloudloadbalancers-us");

      clbApi = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .buildApi(CloudLoadBalancersApi.class);
      lbApi = clbApi.getLoadBalancerApiForZone(ZONE);

      troveApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(TroveApi.class);
      instanceApi = troveApi.getInstanceApiForZone(ZONE);
   }

   /**
    * @return Instance The Instance created in the CreateInstance example.
    */
   private Instance getInstance() {
      for (Instance instance: instanceApi.list()) {
         if (instance.getName().startsWith(NAME)) {
            return instanceApi.get(instance.getId());
         }
      }

      throw new RuntimeException(NAME + " not found. Run the CreateInstance example first.");
   }

   /**
    * @return Returns a set of a single cloud load balancer node that can be used to connect to the database from the public Internet
    */
   private Set<AddNode> addNodesOfDatabaseInstances() {
      AddNode addNode01 = AddNode.builder()
                                 .address(getInstance().getHostname())
                                 .condition(Node.Condition.ENABLED)
                                 .port(3306)
                                 .build();

      return Sets.newHashSet(addNode01);
   }

   /**
    * Builds and executes the request to create a load balancer service using a set of nodes.
    *
    * @param addNodes The set of cloud load balancer nodes.
    */
   private void createLoadBalancer(Set<AddNode> addNodes) throws TimeoutException {
      System.out.format("Create Cloud Load Balancer%n");

      CreateLoadBalancer createLB = CreateLoadBalancer.builder()
            .name(NAME)
            .protocol("MYSQL")
            .port(3306)
            .algorithm(LoadBalancer.Algorithm.RANDOM)
            .nodes(addNodes)
            .virtualIPType(VirtualIP.Type.PUBLIC)
            .build();

      // This will fail if the service cannot resolve the hostname of the database instance.
      // This happens when the internal DNS record for the database hostname has not propagated yet. Just retry to fix.
      LoadBalancer loadBalancer;
      do {
         loadBalancer = lbApi.create(createLB);
         Uninterruptibles.sleepUninterruptibly(30, TimeUnit.SECONDS);
      }
      while(loadBalancer == null);


      // Wait for the Load Balancer to become Active before moving on.
      // If you want to know what's happening during the polling, enable logging. See
      // /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      // Even when the load balancer returns active, it might take a while before connections to the database are possible.
      if (!LoadBalancerPredicates.awaitAvailable(lbApi).apply(loadBalancer)) {
         throw new TimeoutException("Timeout on loadBalancer: " + loadBalancer);
      }

      System.out.format("  %s%n", loadBalancer);
   }

   private String getVirtualIPv4(Set<VirtualIPWithId> set) {
      for (VirtualIPWithId virtualIP: set) {
         if (virtualIP.getType().equals(VirtualIP.Type.PUBLIC) &&
             virtualIP.getIpVersion().equals(VirtualIP.IPVersion.IPV4)) {
            return virtualIP.getAddress();
         }
      }

      throw new RuntimeException("Public IPv4 address not found.");
   }

   /**
    * @return LoadBalancer The LoadBalancer created in this example.
    */
   private LoadBalancer getLb() {
      for (LoadBalancer ls : lbApi.list().concat()) {
         if (ls.getName().startsWith(NAME)) {
            return ls;
         }
      }

      throw new RuntimeException(NAME + " not found. Run the CreateInstance example first.");
   }

   /**
    * Connects to the database using JDBC over the load balancer and executes a simple query without creating a database table.
    * This will verify that the database engine is running on the remote instance.
    *
    * @return true if connection successful and database engine responsive.
    */
   private boolean testDatabase() throws TimeoutException {
      System.out.format("Connect to database%n");

      // See http://dev.mysql.com/doc/refman/5.6/en/connector-j-examples.html
      Connection conn;

      try {
         StringBuilder connString = new StringBuilder();
         connString.append("jdbc:mysql://"); // Begin building the JDBC connection string by specifying the database type.
         connString.append(getVirtualIPv4(getLb().getVirtualIPs())); // IPv4 of cloud load balancer that will be used to connect to the database
         connString.append("/");
         connString.append(NAME); // Database name
         connString.append("?user=");
         connString.append(NAME); // User name
         connString.append("&password=");
         connString.append(PASSWORD); // Database user password

         System.out.format("  Connecting to %s%n", connString); // remove this in your code, never echo credentials

         conn = DriverManager.getConnection(connString.toString());

         Statement stmt = null;
         ResultSet rs = null;

         try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT 3+5"); // A simple query that tests the engine but creates no tables and is fairly fast.
            rs.first();

            System.out.format("  3+5 is %s%n", rs.getInt(1));
         }
         catch (SQLException e){
            System.out.format("SQLException: %s%n", e.getMessage());
            System.out.format("SQLState: %s%n", e.getSQLState());
            System.out.format("VendorError: %s%n", e.getErrorCode());
            e.printStackTrace();

            return false;
         }
         finally {
            // Release resources in reverse order of creation.

            if (rs != null) {
               try {
                  rs.close();
               }
               catch (SQLException sqlEx) {
                  // Ignore - you might get an exception if closing out of order.
               }
            }

            if (stmt != null) {
               try {
                  stmt.close();
               }
               catch (SQLException sqlEx) {
                  // Ignore - you might get an exception if closing out of order.
               }
            }

            if(conn != null) {
               try {
                  conn.close();
               }
               catch (SQLException sqlEx) {
                  // Ignore - rare bugs not necessarily related to a specific database.
               }
            }
         }
      }
      catch (SQLException e) {
         System.out.format("SQLException: %s%n", e.getMessage());
         System.out.format("SQLState: %s%n", e.getSQLState());
         System.out.format("VendorError: %s%n", e.getErrorCode());
         e.printStackTrace();

         return false;
      }

      return true;
   }

   /**
    * Always close your service when you're done with it.
    *
    * Note that closing quietly like this is not necessary in Java 7.
    * You would use try-with-resources in the main method instead.
    */
   public void close() throws IOException {
      if(lbApi != null) {
         lbApi.delete(getLb().getId());
      }
      Closeables.close(troveApi, true);
      Closeables.close(clbApi, true);
   }
}
