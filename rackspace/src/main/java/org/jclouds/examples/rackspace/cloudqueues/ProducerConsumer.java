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
package org.jclouds.examples.rackspace.cloudqueues;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.openstack.marconi.v1.MarconiApi;
import org.jclouds.openstack.marconi.v1.domain.CreateMessage;
import org.jclouds.openstack.marconi.v1.domain.Message;
import org.jclouds.openstack.marconi.v1.features.ClaimApi;
import org.jclouds.openstack.marconi.v1.features.MessageApi;
import org.jclouds.openstack.marconi.v1.features.QueueApi;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.String.format;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.CONSUMER_ID;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.MESSAGE_NUM;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.MESSAGE_TEXT;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.NAME;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.NUM_THREADS;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.PRODUCER_ID;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.PRODUCER_NAME;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.ZONE;

/**
 * Setting up a Producer/Consumer model in Cloud Queues consists of posting messages to your queue, consumers claiming
 * messages from that queue, and then deleting the completed message.
 *
 * The producer-consumer mode has the following characteristics:
 *
 * 1. Messages are acted upon by one (and only one) worker.
 * 2. Worker must delete message when done.
 * 3. TTL restores message to unclaimed state if worker never finishes.
 * 4. Ideal for dispatching jobs to multiple processors.
 *
 * This mode is ideal for dispatching jobs to multiple processors.
 */
public class ProducerConsumer implements Closeable {
   private final MarconiApi marconiApi;
   private final QueueApi queueApi;

   /**
    * To get a username and API key see
    * http://apache.jclouds.org/documentation/quickstart/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      ProducerConsumer producerConsumer = new ProducerConsumer(args[0], args[1]);

      try {
         producerConsumer.createQueue();
         producerConsumer.produceAndConsume();
         producerConsumer.deleteQueue();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         producerConsumer.close();
      }
   }

   public ProducerConsumer(String username, String apiKey) {
      // If this application we're running *inside* the Rackspace Cloud, you would want to use the InternalUrlModule
      // as below to have all of the Cloud Queues traffic go over the internal Rackspace Cloud network.
      // Iterable<Module> modules = ImmutableSet.<Module> of(new InternalUrlModule());

      marconiApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            // .modules(modules)
            .buildApi(MarconiApi.class);
      queueApi = marconiApi.getQueueApiForZoneAndClient(ZONE, PRODUCER_ID);
   }

   private void createQueue() {
      queueApi.create(NAME);
   }

   private void produceAndConsume() throws ExecutionException, InterruptedException {
      System.out.format("Producer Consumer%n");

      ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);

      executorService.execute(new Consumer("1"));
      executorService.execute(new Consumer("2"));

      Future producerFuture = executorService.submit(new Producer("1"));
      producerFuture.get();

      executorService.shutdown();
   }

   private void deleteQueue() {
      queueApi.delete(NAME);
   }

   /**
    * Always close your service when you're done with it.
    *
    * Note that closing quietly like this is not necessary in Java 7.
    * You would use try-with-resources in the main method instead.
    */
   public void close() throws IOException {
      Closeables.close(marconiApi, true);
   }

   private void sleep(long millis) {
      try {
         Thread.sleep(millis);
      }
      catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   public class Producer implements Runnable {
      private final String producerName;
      private final MessageApi messageApi;

      protected Producer(String producerName) {
         this.producerName = producerName;
         messageApi = marconiApi.getMessageApiForZoneAndClientAndQueue(ZONE, PRODUCER_ID, NAME);
      }

      public void run() {
         for (int i = 0; i < 32; i++) {
            messageApi.create(produce(i));
            sleep(250);
         }
      }

      private List<CreateMessage> produce(int messageNum) {
         StringBuilder bodyBuilder = new StringBuilder();
         bodyBuilder.append(format("%s=%s%n", PRODUCER_NAME, producerName))
                    .append(format("%s=%d%n", MESSAGE_NUM, messageNum))
                    .append(format("%s=%s%n", MESSAGE_TEXT, "Queue This Way"));

         CreateMessage message = CreateMessage.builder().ttl(300).body(bodyBuilder.toString()).build();

         System.out.format("  Producer %s Message %s:%d%n", producerName, producerName, messageNum);

         return ImmutableList.of(message);
      }
   }

   public class Consumer implements Runnable {
      private final String consumerName;
      private final MessageApi messageApi;
      private final ClaimApi claimApi;

      protected Consumer(String consumerName) {
         this.consumerName = consumerName;
         messageApi = marconiApi.getMessageApiForZoneAndClientAndQueue(ZONE, CONSUMER_ID, NAME);
         claimApi = marconiApi.getClaimApiForZoneAndClientAndQueue(ZONE, CONSUMER_ID, NAME);
      }

      public void run() {
         for (int i = 0; i < 32; i++) {
            List<Message> messages = claimApi.claim(120, 60, 2);
            consume(messages);
            sleep(300);
         }
      }

      private void consume(List<Message> messages) {
         for (Message message : messages) {
            Properties props = loadStringProperties(message.getBody());

            System.out.format("  Consumer %s Message %s:%s (%s)%n", consumerName,
                  props.getProperty(PRODUCER_NAME), props.getProperty(MESSAGE_NUM), props.getProperty(MESSAGE_TEXT));

            messageApi.deleteByClaim(message.getId(), message.getClaimId().get());
         }
      }

      private Properties loadStringProperties(String body) {
         Properties properties = new Properties();

         try {
            properties.load(new StringReader(body));
         }
         catch (IOException e) {
            // IOException will never occur here because we're loading directly from a String
         }

         return properties;
      }
   }
}
