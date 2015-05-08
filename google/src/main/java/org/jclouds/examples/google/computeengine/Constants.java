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
package org.jclouds.examples.google.computeengine;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Constants used by the Google Compute Engine Examples.
 */
public interface Constants {
   public static final String PROVIDER = System.getProperty("provider.cs", "google-compute-engine");
   public static final String ZONE = System.getProperty("zone", "europe-west1-b");
   public static final String PROFILE = System.getProperty("profile", "f1-micro");
   public static final String IMAGE = System.getProperty("image", "debian-7-wheezy");
   public static final String NAME = "jclouds-example";
   public static final String POLL_PERIOD_TWENTY_SECONDS = String.valueOf(SECONDS.toMillis(20));
}
