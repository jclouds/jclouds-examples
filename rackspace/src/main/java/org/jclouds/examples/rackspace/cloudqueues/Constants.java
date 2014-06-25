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

import java.util.UUID;

/**
 * Constants used by the Rackspace Examples.
 */
public interface Constants {
   // The provider configures jclouds To use the Rackspace Cloud (US)
   // To use the Rackspace Cloud (UK) set the system property or default value to "rackspace-cloudqueues-uk"
   final String PROVIDER = System.getProperty("provider.cbs", "rackspace-cloudqueues-us");
   final String ZONE = System.getProperty("zone", "IAD");

   final UUID PRODUCER_ID = UUID.fromString("3381af92-2b9e-11e3-b191-71861300734a");
   final UUID CONSUMER_ID = UUID.fromString("3381af92-2b9e-11e3-b191-71861300734b");
   final UUID PUBLISHER_ID = UUID.fromString("3381af92-2b9e-11e3-b191-71861300734c");
   final UUID SUBSCRIBER_ID = UUID.fromString("3381af92-2b9e-11e3-b191-71861300734d");
   final String NAME = "jclouds-example";

   final int NUM_THREADS = 3;

   final String PRODUCER_NAME = "producer.name";
   final String PUBLISHER_NAME = "publisher.name";
   final String MESSAGE_TEXT = "message.text";
   final String MESSAGE_NUM = "message.num";
}
