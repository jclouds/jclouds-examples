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
package org.jclouds.examples.dimensiondata.cloudcontrol;

import com.google.common.base.Predicate;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.jclouds.dimensiondata.cloudcontrol.domain.Server;
import org.jclouds.dimensiondata.cloudcontrol.domain.Vlan;

public class WaitForUtils
{

    private static final String SERVER_STARTED_PREDICATE = "SERVER_STARTED_PREDICATE";
    private static final String SERVER_NORMAL_PREDICATE = "SERVER_NORMAL_PREDICATE";
    private static final String NETWORK_DOMAIN_NORMAL_PREDICATE = "NETWORK_DOMAIN_NORMAL_PREDICATE";
    private static final String VLAN_NORMAL_PREDICATE = "VLAN_NORMAL_PREDICATE";
    private static final String SERVER_DELETED_PREDICATE = "SERVER_DELETED_PREDICATE";
    private static final String NETWORK_DOMAIN_DELETED_PREDICATE = "NETWORK_DOMAIN_DELETED_PREDICATE";
    private static final String VLAN_DELETED_PREDICATE = "VLAN_DELETED_PREDICATE";
    private static final String SERVER_STOPPED_PREDICATE = "SERVER_STOPPED_PREDICATE";

    static void waitForServerStopped(Injector injector, Server server)
    {
        Predicate<String> serverStoppedPredicate = injector.getInstance(Key.get(new TypeLiteral<Predicate<String>>()
        {
        }, Names.named(SERVER_STOPPED_PREDICATE)));

        // Wait for Server to be STOPPED
        serverStoppedPredicate.apply(server.id());
    }

    static void waitForDeleteVlan(Injector injector, Vlan vlan)
    {
        Predicate<String> vlanDeletedPredicate = injector.getInstance(Key.get(new TypeLiteral<Predicate<String>>()
        {
        }, Names.named(VLAN_DELETED_PREDICATE)));

        // Wait for VLAN to be DELETED
        vlanDeletedPredicate.apply(vlan.id());
    }

    static void waitForDeleteNetworkDomain(Injector injector, String networkDomainId)
    {
        Predicate<String> networkDomainDeletedPredicate = injector.getInstance(Key.get(new TypeLiteral<Predicate<String>>()
        {
        }, Names.named(NETWORK_DOMAIN_DELETED_PREDICATE)));

        // Wait for NETWORK DOMAIN to be DELETED
        networkDomainDeletedPredicate.apply(networkDomainId);
    }

    static void waitForServerDeleted(Injector injector, Server server)
    {
        Predicate<String> serverDeletedPredicate = injector.getInstance(Key.get(new TypeLiteral<Predicate<String>>()
        {
        }, Names.named(SERVER_DELETED_PREDICATE)));

        // Wait for Server to be DELETED
        serverDeletedPredicate.apply(server.id());
    }

    static void waitForServerStartedAndNormal(Injector injector, String serverId)
    {
        Predicate<String> serverStartedPredicate = injector.getInstance(Key.get(new TypeLiteral<Predicate<String>>()
        {
        }, Names.named(SERVER_STARTED_PREDICATE)));
        Predicate<String> serverNormalPredicate = injector.getInstance(Key.get(new TypeLiteral<Predicate<String>>()
        {
        }, Names.named(SERVER_NORMAL_PREDICATE)));

        // Wait for Server to be started and NORMAL
        serverStartedPredicate.apply(serverId);
        serverNormalPredicate.apply(serverId);
    }

    static void waitForNetworkDomainNormal(Injector injector, String networkDomainId)
    {
        Predicate<String> networkDomainNormalPredicate = injector.getInstance(Key.get(new TypeLiteral<Predicate<String>>()
        {
        }, Names.named(NETWORK_DOMAIN_NORMAL_PREDICATE)));
        networkDomainNormalPredicate.apply(networkDomainId);
    }

    static void waitForVlanNormal(Injector injector, String vlanId)
    {
        Predicate<String> vlanNormalPredicate = injector.getInstance(Key.get(new TypeLiteral<Predicate<String>>()
        {
        }, Names.named(VLAN_NORMAL_PREDICATE)));
        vlanNormalPredicate.apply(vlanId);
    }
}
