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
import com.google.common.collect.Lists;
import org.jclouds.ContextBuilder;
import org.jclouds.dimensiondata.cloudcontrol.DimensionDataCloudControlApi;
import org.jclouds.dimensiondata.cloudcontrol.domain.Disk;
import org.jclouds.dimensiondata.cloudcontrol.domain.NIC;
import org.jclouds.dimensiondata.cloudcontrol.domain.NetworkInfo;
import org.jclouds.dimensiondata.cloudcontrol.domain.TagInfo;
import org.jclouds.dimensiondata.cloudcontrol.options.DatacenterIdListFilters;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.rest.ApiContext;

import java.util.Collections;
import java.util.List;

/**
 * This class will attempt to Deploy:
 * <ul>
 * <li>Network Domain</li>
 * <li>Vlan</li>
 * <li>Server</li>cd ..
 * </ul>
 * <p>
 * For each of these deployed assets we will tag them so that we know they were created by jclouds.
 */
public class DeployNetworkDomainVlanAndServer
{

    private static final String ZONE = System.getProperty("jclouds.zone", "AU9");
    private static final String DIMENSIONDATA_CLOUDCONTROL_PROVIDER = "dimensiondata-cloudcontrol";

    public static void main(String[] args)
    {
        String endpoint = args[0];
        String username = args[1];
        String password = args[2];
        /*
         * Build an instance of the Dimension DataCloud Control Provider using the endpoint provided.
         * Typically the endpoint will be of the form https://api-GEO.dimensiondata.com/caas
         * We also need to provide authenticate details, a username and password.
         *
         * Internally the Dimension Data CloudControl Provider will use the org.jclouds.dimensiondata.cloudcontrol.features.AccountApi
         * to lookup the organization identifier so that it is used as part of the requests.
         *
         */
        try (ApiContext<DimensionDataCloudControlApi> ctx = ContextBuilder.newBuilder(DIMENSIONDATA_CLOUDCONTROL_PROVIDER)
                .endpoint(endpoint)
                .credentials(username, password)
                .modules(ImmutableSet.of(new SLF4JLoggingModule()))
                .build())
        {
            DimensionDataCloudControlApi api = ctx.getApi();

            /*
             * Create a tag key. We will use this to tag the assets that we create.
             */
            String tagKeyId = api.getTagApi().createTagKey("jclouds", "owner of the asset", true, false);

            String networkDomainId = deployNetworkDomain(api, tagKeyId);
            String vlanId = deployVlan(api, networkDomainId, tagKeyId);

            deployServer(api, networkDomainId, vlanId, tagKeyId);
        }

    }

    private static void deployServer(DimensionDataCloudControlApi api, String networkDomainId, String vlanId, String tagKeyId)
    {
        /*
         * The server we deploy will use a pre-configured image.
         *
         * In Dimension Data Cloud Control we support OS Images and
         * Customer Images (user created using the org.jclouds.dimensiondata.cloudcontrol.features.ServerApi.cloneServer operation)
         */
        String imageId = getOsImage(api);

        /*
         * The Server that gets deployed will have some network configuration. It gets assigned to the Vlan that was created previously.
         */
        NetworkInfo networkInfo = NetworkInfo
                .create(networkDomainId, NIC.builder().vlanId(vlanId).build(), Lists.<NIC>newArrayList());
        /*
         * The Server that gets deployed will have some additional disk configuration.
         */
        List<Disk> disks = ImmutableList.of(Disk.builder().scsiId(0).speed("STANDARD").build());

        /*
         * The Server is deployed using the OS Image we selected,
         * a flag to signal if we want it started or not, an admin pass and the additional configuration we built.
         */
        String serverId = api.getServerApi()
                .deployServer("jclouds-server", imageId, true, networkInfo, "P$$ssWwrrdGoDd!", disks, null);

        /*
         * A Server deployment is an asynchronous process. We need to wait for it to complete. The Dimension Data provider
         * has built in predicates that will block execution and check that the Server's State has moved from PENDING_ADD to NORMAL.
         */
       api.getServerApi().serverStartedPredicate().apply(serverId);
       api.getServerApi().serverNormalPredicate().apply(serverId);

        /*
         * Apply a Tag to the Server. We use AssetType SERVER.
         * We pass in the tagKeyId and a value that we want to associate, in this case jclouds.
         */
        api.getTagApi().applyTags(serverId, "SERVER", Collections.singletonList(TagInfo.create(tagKeyId, "jclouds")));
    }

    private static String getOsImage(DimensionDataCloudControlApi api)
    {
        /*
         * We list available OS Images filtering on the Region (Datacenter) we wish to operate on.
         */
        return api.getServerImageApi().listOsImages(DatacenterIdListFilters.Builder.datacenterId(ZONE)).iterator().next().id();
    }

    private static String deployNetworkDomain(DimensionDataCloudControlApi api, String tagKeyId)
    {

        /*
         * Deploy Network Domain to the Region we wish to operate on. The response from this API is the Network Domain Identifier.
         */
        String networkDomainId = api.getNetworkApi().deployNetworkDomain(ZONE, "jclouds-example", "jclouds-example", "ESSENTIALS");

        /*
         * A Network Domain deployment is an asynchronous process. We need to wait for it to complete. The Dimension Data provider
         * has built in predicates that will block execution and check that the Network Domain's State has moved from PENDING_ADD to NORMAL.
         * We pass the Network Domain Identifier we wish to check the state of.
         */
       api.getNetworkApi().networkDomainNormalPredicate().apply(networkDomainId);

        /*
         * Apply a Tag to the Network Domain. We use AssetType NETWORK_DOMAIN.
         * We pass in the tagKeyId and a value that we want to associate, in this case jclouds.
         */
        api.getTagApi().applyTags(networkDomainId, "NETWORK_DOMAIN", Collections.singletonList(TagInfo.create(tagKeyId, "jclouds")));
        return networkDomainId;
    }

    private static String deployVlan(DimensionDataCloudControlApi api, String networkDomainId, String tagKeyId)
    {

        /*
         * Deploy the Vlan and associate it with the Network Domain that was previously created.
         * The Vlan is deployed with a privateIpv4BaseAddress and privateIpv4PrefixSize
         */
        String vlanId = api.getNetworkApi().deployVlan(networkDomainId, "jclouds-example-vlan", "jclouds-example-vlan", "10.0.0.0", 24);

        /*
         * A Vlan deployment is an asynchronous process. We need to wait for it to complete. The Dimension Data provider
         * has built in predicates that will block execution and check that the Vlan's State has moved from PENDING_ADD to NORMAL.
         */
       api.getNetworkApi().vlanNormalPredicate().apply(vlanId);

        /*
         * Apply a Tag to the Vlan. We use AssetType VLAN.
         * We pass in the tagKeyId and a value that we want to associate, in this case jclouds.
         */
        api.getTagApi().applyTags(vlanId, "VLAN", Collections.singletonList(TagInfo.create(tagKeyId, "jclouds")));
        return vlanId;
    }

}
