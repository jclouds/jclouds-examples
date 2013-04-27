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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jclouds.rackspace.clouddns.v1.domain.Domain;

import com.google.common.base.Function;
import com.google.common.base.Predicate;


/**
 * Constants used by the Rackspace Examples.
 * 
 * @author Everett Toews
 */
public interface Constants {
   public static final String NAME = "jclouds-example.com";
   public static final String ALT_NAME = "alt-jclouds-example.com";
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
