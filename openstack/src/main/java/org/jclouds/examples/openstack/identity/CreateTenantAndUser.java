/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.examples.openstack.identity;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.keystone.v2_0.KeystoneApi;
import org.jclouds.openstack.keystone.v2_0.domain.Tenant;
import org.jclouds.openstack.keystone.v2_0.domain.User;
import org.jclouds.openstack.keystone.v2_0.extensions.TenantAdminApi;
import org.jclouds.openstack.keystone.v2_0.extensions.UserAdminApi;
import org.jclouds.openstack.keystone.v2_0.options.CreateTenantOptions;
import org.jclouds.openstack.keystone.v2_0.options.CreateUserOptions;

import java.io.Closeable;
import java.io.IOException;

public class CreateTenantAndUser implements Closeable {
   private final KeystoneApi keystoneApi;

   /**
    * The first argument (args[0]) must be your Identity (Keystone) endpoint (e.g. an IP address or URL)
    * The second argument (args[1]) must be your tenant name
    * The third argument (args[2]) must be your user name
    * The fourth argument (args[3]) must be your password
    *
    * For this example your endpoint must be the *admin endpoint* of your Identity service
    * (e.g. "http://111.222.333.444:35357/v2.0/")
    */
   public static void main(String[] args) throws IOException {
      CreateTenantAndUser createTenantAndUser = new CreateTenantAndUser(args[0], args[1], args[2], args[3]);

      try {
         Tenant tenant = createTenantAndUser.createTenant();
         createTenantAndUser.createUser(tenant);
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         createTenantAndUser.close();
      }
   }

   public CreateTenantAndUser(String endpoint, String tenantName, String userName, String password) {
      System.out.format("%s%n", this.getClass().getName());

      Iterable<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());

      String provider = "openstack-keystone";
      String identity = tenantName + ":"  + userName;

      keystoneApi = ContextBuilder.newBuilder(provider)
            .endpoint(endpoint)
            .credentials(identity, password)
            .modules(modules)
            .buildApi(KeystoneApi.class);
   }

   private Tenant createTenant() {
      System.out.format("  Create Tenant%n");

      Optional<? extends TenantAdminApi> tenantAdminApiExtension = keystoneApi.getTenantAdminApi();

      if (tenantAdminApiExtension.isPresent()) {
         System.out.format("    TenantAdminApi is present%n");

         TenantAdminApi tenantAdminApi = tenantAdminApiExtension.get();
         CreateTenantOptions tenantOptions = CreateTenantOptions.Builder
               .description("My New Tenant");
         Tenant tenant = tenantAdminApi.create("newTenant", tenantOptions);

         System.out.format("    %s%n", tenant);

         return tenant;
      } else {
         System.out.format("    TenantAdminApi is *not* present%n");
         System.exit(1);

         return null;
      }
   }

   private void createUser(Tenant tenant) {
      System.out.format("  Create User%n");

      Optional<? extends UserAdminApi> userAdminApiExtension = keystoneApi.getUserAdminApi();

      if (userAdminApiExtension.isPresent()) {
         System.out.format("    UserAdminApi is present%n");

         UserAdminApi userAdminApi = userAdminApiExtension.get();
         CreateUserOptions userOptions = CreateUserOptions.Builder
               .tenant(tenant.getId())
               .email("new.email@example.com");
         User user = userAdminApi.create("newUser", "newPassword", userOptions);

         System.out.format("    %s%n", user);
      } else {
         System.out.format("    UserAdminApi is *not* present%n");
         System.exit(1);
      }
   }

   public void close() throws IOException {
      Closeables.close(keystoneApi, true);
   }
}