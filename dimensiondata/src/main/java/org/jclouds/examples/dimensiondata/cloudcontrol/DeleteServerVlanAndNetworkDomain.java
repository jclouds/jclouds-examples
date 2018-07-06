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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import org.jclouds.ContextBuilder;
import org.jclouds.dimensiondata.cloudcontrol.DimensionDataCloudControlApi;
import org.jclouds.dimensiondata.cloudcontrol.domain.Server;
import org.jclouds.dimensiondata.cloudcontrol.domain.TagKey;
import org.jclouds.dimensiondata.cloudcontrol.domain.Vlan;
import org.jclouds.dimensiondata.cloudcontrol.options.DatacenterIdListFilters;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.rest.ApiContext;

import static org.jclouds.examples.dimensiondata.cloudcontrol.WaitForUtils.*;

/**
 * This class will attempt to delete the assets created in org.jclouds.examples.dimensiondata.cloudcontrol.DeployNetworkDomainVlanAndServer:
 * <ul>
 * <li>Server</li>
 * <li>Vlan</li>
 * <li>Network Domain</li>
 * <li>Tag Key</li>
 * </ul>
 */
public class DeleteServerVlanAndNetworkDomain
{
    private static final String ZONE = System.getProperty("jclouds.zone", "AU9");
    private static final String DIMENSIONDATA_CLOUDCONTROL_PROVIDER = "dimensiondata-cloudcontrol";

    public static void main(String[] args)
    {
        /*
         * Build an instance of the Dimension DataCloud Control Provider using the endpoint provided.
         * Typically the endpoint will be of the form https://api-GEO.dimensiondata.com/caas
         * We also need to provide authenticate details, a username and password.
         *
         * Internally the Dimension Data CloudControl Provider will use the org.jclouds.dimensiondata.cloudcontrol.features.AccountApi
         * to lookup the organization identifier so that it is used as part of the requests.
         *
         */
        String endpoint = args[0];
        String username = args[1];
        String password = args[2];

        try (ApiContext<DimensionDataCloudControlApi> ctx = ContextBuilder.newBuilder(DIMENSIONDATA_CLOUDCONTROL_PROVIDER)
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

            /*
             * Referencing the asset created in org.jclouds.examples.dimensiondata.cloudcontrol.DeployNetworkDomainVlanAndServer
             */
            final String networkDomainName = "jclouds-example";
            String networkDomainId = getNetworkDomainId(api, networkDomainName);
            final String serverName = "jclouds-server";
            final String vlanName = "jclouds-example-vlan";

            deleteServer(api, injector, serverName);
            deleteVlan(api, injector, vlanName, networkDomainId);
            deleteNetworkDomain(api, injector, networkDomainId);
            deleteTagKey(api, "jclouds");
        }
    }

    private static void deleteTagKey(DimensionDataCloudControlApi api, final String tagkeyName)
    {
        /*
         * Find the Tag Key and Delete using the id.
         */
        Optional<TagKey> tagKeyOptional = api.getTagApi().listTagKeys().concat().firstMatch(new Predicate<TagKey>()
        {
            @Override
            public boolean apply(TagKey input)
            {
                return input.name().equals(tagkeyName);
            }
        });
        if (tagKeyOptional.isPresent())
        {
            api.getTagApi().deleteTagKey(tagKeyOptional.get().id());
        }
    }

    private static String getNetworkDomainId(DimensionDataCloudControlApi api, final String networkDomainName)
    {
        /*
         * Find the Network Domain that was deployed by doing a filtered lookup using the datacenter and the network domain name.
         */
        return api.getNetworkApi().listNetworkDomainsWithDatacenterIdAndName(ZONE, networkDomainName).concat().toList().get(0).id();
    }

    private static void deleteVlan(DimensionDataCloudControlApi api, Injector injector, final String vlanName, String networkDomainId)
    {
        /*
         * Find the Vlan that was deployed by listing all Vlans for the Network Domain and filtering by name
         */
        Optional<Vlan> vlanOptional = api.getNetworkApi().listVlans(networkDomainId).concat().firstMatch(new Predicate<Vlan>()
        {
            @Override
            public boolean apply(Vlan input)
            {
                return input.name().equals(vlanName);
            }
        });
        if (vlanOptional.isPresent())
        {
            Vlan vlan = vlanOptional.get();

            /*
             * Delete the Vlan using the id.
             */
            api.getNetworkApi().deleteVlan(vlan.id());

            /*
             * A Vlan delete is an asynchronous process. We need to wait for it to complete. The Dimension Data provider
             * has built in predicates that will block execution and check that the Vlan is not found.
             */
            waitForDeleteVlan(injector, vlan);
        }
    }

    private static void deleteNetworkDomain(DimensionDataCloudControlApi api, Injector injector, String networkDomainId)
    {
        /*
         * Network Domain is deleted using the id.
         */
        api.getNetworkApi().deleteNetworkDomain(networkDomainId);

        /*
         * A Network Domain delete is an asynchronous process. We need to wait for it to complete. The Dimension Data provider
         * has built in predicates that will block execution and check that the Network Domain is not found.
         */
        waitForDeleteNetworkDomain(injector, networkDomainId);
    }

    private static void deleteServer(DimensionDataCloudControlApi api, Injector injector, final String serverName)
    {
        /*
         * We list all servers known to this organisation for the datacenter we are operating on. We filter the one that matches the server name we used to create it.
         */
        Optional<Server> serverOptional = api.getServerApi().listServers(DatacenterIdListFilters.Builder.datacenterId(ZONE)).firstMatch(new Predicate<Server>()
        {
            @Override
            public boolean apply(Server input)
            {
                return input.name().equals(serverName);
            }
        });

        if (serverOptional.isPresent())
        {
            Server server = serverOptional.get();
            if (server.started())
            {
                /*
                 * A Server must not be started in order to delete it. We call the shutdown server operation.
                 */
                api.getServerApi().shutdownServer(server.id());

                /*
                 * A Shutdown Server operation is an asynchronous process. We need to wait for it to complete. The Dimension Data provider
                 * has built in predicates that will block execution and check that the Server is shutdown.
                 */
                waitForServerStopped(injector, server);
            }

            /*
             * Server is deleted using the id.
             */
            api.getServerApi().deleteServer(server.id());

            /*
             * A Server delete is an asynchronous process. We need to wait for it to complete. The Dimension Data provider
             * has built in predicates that will block execution and check that the Server is not found.
             */
            waitForServerDeleted(injector, server);

        }
    }

}
