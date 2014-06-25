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

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.openstack.marconi.v1.MarconiApi;
import org.jclouds.openstack.marconi.v1.domain.CreateMessage;
import org.jclouds.openstack.marconi.v1.domain.Message;
import org.jclouds.openstack.marconi.v1.domain.MessageStream;
import org.jclouds.openstack.marconi.v1.features.MessageApi;
import org.jclouds.openstack.marconi.v1.features.QueueApi;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static java.lang.String.format;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.CONSUMER_ID;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.MESSAGE_NUM;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.MESSAGE_TEXT;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.NAME;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.PRODUCER_ID;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.PRODUCER_NAME;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.PUBLISHER_ID;
import static org.jclouds.examples.rackspace.cloudqueues.Constants.ZONE;
import static org.jclouds.openstack.marconi.v1.options.StreamMessagesOptions.Builder.marker;

/**
 * Stream messages off of a queue. In a very active queue it's possible that you could continuously stream messages
 * indefinitely.
 *
 * You can also resume where you left off by remembering the marker.
 */
public class StreamMessages implements Closeable {
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
      StreamMessages streamMessages = new StreamMessages(args[0], args[1]);

      try {
         streamMessages.createQueue();
         streamMessages.createMessages();
         streamMessages.streamMessages();
         streamMessages.deleteQueue();
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         streamMessages.close();
      }
   }

   public StreamMessages(String username, String apiKey) {
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

   private void createMessages() throws ExecutionException, InterruptedException {
      System.out.format("Create Messages%n");

      MessageApi messageApi = marconiApi.getMessageApiForZoneAndClientAndQueue(ZONE, PRODUCER_ID, NAME);
      List<CreateMessage> createMessages = Lists.newArrayList();

      for (int i=0; i < 10; i++) {
         for (int j=0; j < 10; j++) {
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append(format("%s=%s%n", PRODUCER_NAME, PRODUCER_ID))
                       .append(format("%s=%d%n", MESSAGE_NUM, i*10+j))
                       .append(format("%s=%s%n", MESSAGE_TEXT, "Hear Ye! Hear Ye!"));

            CreateMessage createMessage = CreateMessage.builder().ttl(300).body(bodyBuilder.toString()).build();
            createMessages.add(createMessage);
         }

         messageApi.create(createMessages);

         System.out.format("  Created %d messages%n", createMessages.size());

         createMessages.clear();
      }
   }

   private void streamMessages() {
      System.out.format("Stream Messages%n");

      MessageApi messageApi = marconiApi.getMessageApiForZoneAndClientAndQueue(ZONE, CONSUMER_ID, NAME);
      MessageStream stream = messageApi.stream();
      String marker = "";

      while(stream.nextMarker().isPresent()) {
         for (Message message: stream) {
            Properties messageProps = loadStringProperties(message.getBody());
            int messageNum = Integer.valueOf(messageProps.getProperty(MESSAGE_NUM));

            System.out.format("  Read message %d%n", messageNum);

            if (messageNum == 49) {
               System.out.format("  Breaking at message %d%n", messageNum);
               // Breaking here to illustrate how to resume using the marker below
               break;
            }
         }

         marker = stream.nextStreamOptions().getMarker();
         stream = messageApi.stream(stream.nextStreamOptions());
      }

      stream = messageApi.stream(marker(marker));

      while(stream.nextMarker().isPresent()) {
         for (Message message: stream) {
            Properties messageProps = loadStringProperties(message.getBody());
            int messageNum = Integer.valueOf(messageProps.getProperty(MESSAGE_NUM));

            System.out.format("  Read message %d%n", messageNum);
         }

         stream = messageApi.stream(stream.nextStreamOptions());
      }
   }

   private void deleteQueue() {
      queueApi.delete(NAME);
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

   /**
    * Always close your service when you're done with it.
    *
    * Note that closing quietly like this is not necessary in Java 7.
    * You would use try-with-resources in the main method instead.
    */
   public void close() throws IOException {
      Closeables.close(marconiApi, true);
   }
}
