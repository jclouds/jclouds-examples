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
package org.jclouds.examples.rackspace.cloudservers;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Sets.filter;
import static org.jclouds.compute.predicates.NodePredicates.TERMINATED;
import static org.jclouds.compute.predicates.NodePredicates.all;
import static org.jclouds.compute.predicates.NodePredicates.inGroup;

import java.util.Map;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.rest.RestContext;

import com.google.common.collect.ImmutableMap;

/**
 * This example sets, gets, updates, and deletes metadata from a server. It also serves as an example of when it's
 * necessary to use the more specific OpenStack NovaApi (as opposed to using the generic jclouds ComputeService) to 
 * do what you need to. Manipulaing server metadata in this case.  
 *  
 * @author Everett Toews
 */
public class ServerMetadata {
	private static final String GROUP_NAME = "jclouds-example";
	
	private ComputeService compute;
	private RestContext<NovaApi, NovaAsyncApi> nova;

	/**
	 * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
	 * 
	 * The first argument (args[0]) must be your username
	 * The second argument (args[1]) must be your API key
	 */
	public static void main(String[] args) {
		ServerMetadata serverMetadata = new ServerMetadata();
		
		try {
			serverMetadata.init(args);
			
			NodeMetadata nodeMetadata = serverMetadata.getServer();
			serverMetadata.setMetadata(nodeMetadata);
			serverMetadata.updateMetadata(nodeMetadata);
			serverMetadata.deleteMetadata(nodeMetadata);
			serverMetadata.getMetadata(nodeMetadata);
		} 
		finally {
			serverMetadata.close();
		}
	}

	private void init(String[] args) {	
		// The provider configures jclouds to use the Rackspace open cloud
		String provider = "rackspace-cloudservers-us";
		
		String username = args[0];
		String apiKey = args[1];

		ComputeServiceContext context = ContextBuilder.newBuilder(provider)
			.credentials(username, apiKey)
			.buildView(ComputeServiceContext.class);
		compute = context.getComputeService();
		nova = context.unwrap();
	}

	/**
	 * @return The NodeMetadata of the server created in the CreateServer example
	 */
	private NodeMetadata getServer() {
		Set<? extends NodeMetadata> servers = 
			 filter(compute.listNodesDetailsMatching(all()), and(inGroup(GROUP_NAME), not(TERMINATED)));
		
		return servers.iterator().next();
	}
	
	private void setMetadata(NodeMetadata nodeMetadata) {
		System.out.println("Set Metadata");
		
		ServerApi serverApi = nova.getApi().getServerApiForZone(nodeMetadata.getLocation().getParent().getId());
		ImmutableMap<String, String> metadata = ImmutableMap.<String, String> of("key1", "value1", "key2", "value2", "key3", "value3");  
		Map<String, String> responseMetadata = serverApi.setMetadata(nodeMetadata.getProviderId(), metadata);
		
		System.out.println("  " + responseMetadata);
	}

	private void updateMetadata(NodeMetadata nodeMetadata) {
		System.out.println("Udpate Metadata");
		
		ServerApi serverApi = nova.getApi().getServerApiForZone(nodeMetadata.getLocation().getParent().getId());
		ImmutableMap<String, String> metadata = ImmutableMap.<String, String> of("key2", "new-value2");  
		Map<String, String> responseMetadata = serverApi.updateMetadata(nodeMetadata.getProviderId(), metadata);
		
		System.out.println("  " + responseMetadata);
	}

	private void deleteMetadata(NodeMetadata nodeMetadata) {
		System.out.println("Delete Metadata");
		
		ServerApi serverApi = nova.getApi().getServerApiForZone(nodeMetadata.getLocation().getParent().getId());
		serverApi.deleteMetadata(nodeMetadata.getProviderId(), "key3");
	}

	private void getMetadata(NodeMetadata nodeMetadata) {
		System.out.println("Get Metadata");
		
		ServerApi serverApi = nova.getApi().getServerApiForZone(nodeMetadata.getLocation().getParent().getId());
		Map<String, String> metadata = serverApi.getMetadata(nodeMetadata.getProviderId());
		
		System.out.println("  " + metadata);
	}

	/**
	 * Always close your service when you're done with it.
	 */
	private void close() {
		if (compute != null) {
			compute.getContext().close();
		}
	}
}
