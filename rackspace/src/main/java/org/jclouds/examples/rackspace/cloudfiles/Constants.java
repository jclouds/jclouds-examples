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
package org.jclouds.examples.rackspace.cloudfiles;

/**
 * Constants used by the Rackspace Examples.
 */
public interface Constants {
   // The provider configures jclouds To use the Rackspace Cloud (US)
   // To use the Rackspace Cloud (UK) set the system property or default value to "rackspace-cloudfiles-uk"
   public static final String PROVIDER = System.getProperty("provider.cf", "rackspace-cloudfiles-us");
   public static final String REGION = System.getProperty("region", "IAD");

   public static final String CONTAINER_PUBLISH = "jclouds-example-publish";
   public static final String CONTAINER = "jclouds-example";
   public static final String FILENAME = "createObjectFromFile";
   public static final String SUFFIX = ".html";
}
