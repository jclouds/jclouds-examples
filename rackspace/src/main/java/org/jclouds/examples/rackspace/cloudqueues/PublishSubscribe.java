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
import org.jclouds.openstack.marconi.v1.domain.MessageStream;
import org.jclouds.openstack.marconi.v1.features.MessageApi;
import org.jclouds.openstack.marconi.v1.features.QueueApi;
import org.jclouds.openstack.marconi.v1.options.StreamMessagesOptions;

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
import static org.jclouds.examples.rackspace.cloudqueues.Constants.MESSAGE_NUM;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.MESSAGE_TEXT;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.NAME;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.NUM_THREADS;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.PUBLISHER_ID;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.PUBLISHER_NAME;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.SUBSCRIBER_ID;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.ZONE;
import static org.jclouds.openstack.marconi.v1.options.StreamMessagesOptions.Builder.limit;

/**
 * Characteristics of the Publish/Subscribe model in Cloud Queues are:
 *
 * 1. All subscribers listen to the messages on the queue.
 * 2. Messages are not claimed.
 * 3. Subscribers can send a marker/cursor to skip messages already seen.
 * 4. TTL deletes messages eventually.
 *
 * Ideal for notification of events to multiple listeners at once.
 */
public class PublishSubscribe implements Closeable {
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
      PublishSubscribe publishSubscribe = new PublishSubscribe(args[0], args[1]);

      try {
         publishSubscribe.createQueue();
         publishSubscribe.publishAndSubscribe();
         publishSubscribe.deleteQueue();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         publishSubscribe.close();
      }
   }

   public PublishSubscribe(String username, String apiKey) {
      // If this application we're running *inside* the Rackspace Cloud, you would want to use the InternalUrlModule
      // as below to have all of the Cloud Queues traffic go over the internal Rackspace Cloud network.
      // Iterable<Module> modules = ImmutableSet.<Module> of(new InternalUrlModule());

      marconiApi = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            // .modules(modules)
            .buildApi(MarconiApi.class);
      queueApi = marconiApi.getQueueApiForZoneAndClient(ZONE, PUBLISHER_ID);
   }

   private void createQueue() {
      queueApi.create(NAME);
   }

   private void publishAndSubscribe() throws ExecutionException, InterruptedException {
      System.out.format("Publisher Subscriber%n");

      ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);

      executorService.execute(new Subscriber("1"));
      executorService.execute(new Subscriber("2"));

      Future publisherFuture = executorService.submit(new Publisher("1"));
      publisherFuture.get();

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

   public class Publisher implements Runnable {
      private final String publisherName;
      private final MessageApi messageApi;

      protected Publisher(String publisherName) {
         this.publisherName = publisherName;
         messageApi = marconiApi.getMessageApiForZoneAndClientAndQueue(ZONE, PUBLISHER_ID, NAME);
      }

      public void run() {
         for (int i = 0; i < 32; i++) {
            messageApi.create(publish(i));
            sleep(200);
         }
      }

      private List<CreateMessage> publish(int messageNum) {
         StringBuilder bodyBuilder = new StringBuilder();
         bodyBuilder.append(format("%s=%s%n", PUBLISHER_NAME, publisherName))
                    .append(format("%s=%d%n", MESSAGE_NUM, messageNum))
                    .append(format("%s=%s%n", MESSAGE_TEXT, "Read all about it"));

         CreateMessage message = CreateMessage.builder().ttl(300).body(bodyBuilder.toString()).build();

         System.out.format("  Publisher  %s Message %s:%d%n", publisherName, publisherName, messageNum);

         return ImmutableList.of(message);
      }
   }

   public class Subscriber implements Runnable {
      private final String subscriberName;
      private final MessageApi messageApi;
      private int consecutiveSleepCount = 0;

      protected Subscriber(String subscriberName) {
         this.subscriberName = subscriberName;
         messageApi = marconiApi.getMessageApiForZoneAndClientAndQueue(ZONE, SUBSCRIBER_ID, NAME);
      }

      /**
       * Process messages off the queue until we haven't seen any messages 3 times in a row.
       */
      public void run() {
         StreamMessagesOptions streamMessagesOptions = limit(2);
         MessageStream stream = messageApi.stream(streamMessagesOptions);

         while (consecutiveSleepCount < 3) {
            if (stream.nextMarker().isPresent()) {
               process(stream);
               consecutiveSleepCount = 0;
               streamMessagesOptions = stream.nextStreamOptions();
            }
            else {
               sleep(150);
               consecutiveSleepCount++;
               // leave the streamMessagesOptions from the previous loop as is so it can be used in the next loop
            }

            stream = messageApi.stream(streamMessagesOptions);
         }
      }

      private void process(MessageStream messageStream) {
         for (Message message : messageStream) {
            Properties props = loadStringProperties(message.getBody());

            System.out.format("  Subscriber %s Message %s:%s (%s)%n", subscriberName,
                  props.getProperty(PUBLISHER_NAME), props.getProperty(MESSAGE_NUM), props.getProperty(MESSAGE_TEXT));
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
