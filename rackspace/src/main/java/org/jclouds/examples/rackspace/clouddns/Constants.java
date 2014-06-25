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
import com.google.common.base.Predicate;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Constants used by the Rackspace Examples.
 */
public interface Constants {
   // The provider configures jclouds To use the Rackspace Cloud (US)
   // To use the Rackspace Cloud (UK) set the system property or default value to "rackspace-clouddns-uk"
   public static final String PROVIDER = System.getProperty("provider.cdns", "rackspace-clouddns-us");

   public static final String NAME = System.getProperty("user.name") + "-jclouds-example.com";
   public static final String ALT_NAME = "alt-" + NAME;
   public static final String CLOUD_SERVERS = "cloudServersOpenStack";

   // subdomains contain two or more '.'
   public static final Pattern SUBDOMAIN_PATTERN = Pattern.compile(".*\\..*\\.");

   /**
    * Determine if a domain is a top level domain (i.e. not a subdomain).
    */
   public static final Predicate<Domain> IS_DOMAIN = new Predicate<Domain>() {
      public boolean apply(Domain domain) {
         Matcher matcher = SUBDOMAIN_PATTERN.matcher(domain.getName());
         return !matcher.find();
       }
   };

   /**
    * Take a Domain and return its id.
    */
   public static final Function<Domain, Integer> GET_DOMAIN_ID = new Function<Domain, Integer>() {
      public Integer apply(Domain domain) {
         return domain.getId();
      }
   };
}
