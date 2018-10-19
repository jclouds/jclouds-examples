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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import org.jclouds.ContextBuilder;
import org.jclouds.dimensiondata.cloudcontrol.DimensionDataCloudControlApi;
import org.jclouds.dimensiondata.cloudcontrol.domain.*;
import org.jclouds.dimensiondata.cloudcontrol.options.DatacenterIdListFilters;
import org.jclouds.logging.Logger;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.rest.ApiContext;

import static org.jclouds.examples.dimensiondata.cloudcontrol.WaitForUtils.*;

/**
 * This example shows how a Network Domain and all of it's associated assets are removed.
 * Takes 4 Program Arguments:
 * <ul>
 * <li>Endpoint URL</li>
 * <li>Usernamme</li>
 * <li>Password</li>
 * <li>Network Domain Id</li>
 * </ul>
 */
public class NetworkDomainTearDown
{
    private static final Logger logger = Logger.CONSOLE;

    public static void main(String[] args)
    {
        String provider = "dimensiondata-cloudcontrol";
        String endpoint = args[0];
        String username = args[1];
        String password = args[2];
        String networkDomainId = args[3];

        try (ApiContext<DimensionDataCloudControlApi> ctx = ContextBuilder.newBuilder(provider)
                .endpoint(endpoint)
                .credentials(username, password)
                .modules(ImmutableSet.of(new SLF4JLoggingModule()))
                .build())
        {

            /*
             * Retrieve the Guice injector from the context.
             * We will use this for retrieving the some Predicates that are used by the following operations.
             */
            Injector injector = ctx.utils().injector();
            DimensionDataCloudControlApi api = ctx.getApi();


            logger.info("Deleting resources for network domain %s", networkDomainId);
            NetworkDomain networkDomain = api.getNetworkApi().getNetworkDomain(networkDomainId);
            if (networkDomain == null)
            {
                logger.info("Network Domain with Id %s is not found", networkDomainId);
                return;
            }
            if (networkDomain.state() != State.NORMAL)
            {
                logger.info("Network Domain with Id %s is not in a NORMAL state, cannot delete", networkDomain.id());
                return;
            }

            String datacenterId = networkDomain.datacenterId();

            removePublicIpBlocks(networkDomainId, api);

            deleteNatRules(networkDomainId, api);

            deleteFirewallRules(networkDomainId, api);

            deleteServers(api, injector, datacenterId);

            ImmutableList<Server> servers = api.getServerApi().listServers().concat().toList();
            if (!servers.isEmpty())
            {
                logger.info("Could not delete all Servers. Servers not deleted:");
                for (Server server : servers)
                {
                    logger.info("Id %s, Name %s, State, %s", server.id(), server.name(), server.state());
                }
                return;
            }
            deleteVlans(api, injector, networkDomain);

            deleteNetworkDomain(networkDomainId, api, injector);
        }
    }

    private static void removePublicIpBlocks(String networkDomainId, DimensionDataCloudControlApi api)
    {
        for (PublicIpBlock publicIpBlock : api.getNetworkApi().listPublicIPv4AddressBlocks(networkDomainId).concat().toList())
        {
            logger.info("Deleting PublicIpBlock with Id %s", publicIpBlock.id());
            api.getNetworkApi().removePublicIpBlock(publicIpBlock.id());
        }
    }

    private static void deleteFirewallRules(String networkDomainId, DimensionDataCloudControlApi api)
    {
        for (FirewallRule firewallRule : api.getNetworkApi().listFirewallRules(networkDomainId).concat().toList())
        {
            if (firewallRule.ruleType().equals("CLIENT_RULE"))
            {
                logger.info("Deleting FirewallRule with Id %s", firewallRule.id());
                api.getNetworkApi().deleteFirewallRule(firewallRule.id());
            }
        }
    }

    private static void deleteNatRules(String networkDomainId, DimensionDataCloudControlApi api)
    {
        for (NatRule natRule : api.getNetworkApi().listNatRules(networkDomainId).concat().toList())
        {
            logger.info("Deleting NatRule with Id %s", natRule.id());
            api.getNetworkApi().deleteNatRule(natRule.id());
        }
    }

    private static void deleteNetworkDomain(String networkDomainId, DimensionDataCloudControlApi api, Injector injector)
    {
        logger.info("Deleting Network Domain with Id %s", networkDomainId);
        api.getNetworkApi().deleteNetworkDomain(networkDomainId);
        waitForDeleteNetworkDomain(injector, networkDomainId);
    }

    private static void deleteVlans(DimensionDataCloudControlApi api, Injector injector, NetworkDomain networkDomain)
    {
        for (Vlan vlan : api.getNetworkApi().listVlans(networkDomain.id()).concat().toList())
        {
            try
            {
                if (vlan.state() != State.NORMAL)
                {
                    logger.info("Vlan with Id %s is not in a NORMAL state, cannot delete", vlan.id());
                    continue;
                }
                logger.info("Deleting Vlan with Id %s", vlan.id());
                api.getNetworkApi().deleteVlan(vlan.id());
                waitForDeleteVlan(injector, vlan);
            }
            catch (Exception e)
            {
                logger.error("Unable to delete Vlan with Id %s due to: %s", vlan.id(), e.getMessage());
            }
        }
    }

    private static void deleteServers(DimensionDataCloudControlApi api, Injector injector, String datacenterId)
    {
        for (Server server : api.getServerApi().listServers(DatacenterIdListFilters.Builder.datacenterId(datacenterId)))
        {
            try
            {
                if (server.state() == State.FAILED_ADD)
                {
                    logger.info("Server with Id %s is in a FAILED_ADD state, running the clean server operation.", server.id());
                    api.getServerApi().cleanServer(server.id());
                    waitForServerDeleted(injector, server);
                    if (api.getServerApi().getServer(server.id()) != null)
                    {
                        logger.info("Failed to clean Server with Id %s", server.id());
                        continue;
                    }
                }
                if (server.state() != State.NORMAL)
                {
                    logger.info("Server with Id %s is not in a NORMAL state, current state %s - cannot delete", server.id(), server.state());
                    continue;
                }
                if (server.started())
                {
                    logger.info("Shutting down Server with Id %s", server.id());
                    api.getServerApi().shutdownServer(server.id());
                    waitForServerStopped(injector, server);
                }
                logger.info("Deleting Server with Id %s", server.id());
                api.getServerApi().deleteServer(server.id());
                waitForServerDeleted(injector, server);
            }
            catch (Exception e)
            {
                logger.error("Unable to Delete Server with Id %s due to: %s", server.id(), e.getMessage());
            }
        }
    }

}
