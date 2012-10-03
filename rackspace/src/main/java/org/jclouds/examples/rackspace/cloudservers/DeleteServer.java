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

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.rest.RestContext;

import com.google.common.collect.FluentIterable;

/**
 * This example destroys the server created in the CreateServer example. 
 *  
 * @author Everett Toews
 */
public class DeleteServer {
	private static final String SERVER_NAME = "jclouds-example";
	private static final String ZONE = "DFW";
	
	private ComputeService compute;
	private RestContext<NovaApi, NovaAsyncApi> nova;

	/**
	 * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
	 * 
	 * The first argument (args[0]) must be your username
	 * The second argument (args[1]) must be your API key
	 */
	public static void main(String[] args) {
		DeleteServer deleteServer = new DeleteServer();
		
		try {
			deleteServer.init(args);
			deleteServer.deleteServer();
		} 
		finally {
			deleteServer.close();
		}
	}

	private void init(String[] args) {	
		// The provider configures jclouds to use the Rackspace open cloud (US)
		// to use the Rackspace open cloud (UK) set the provider to "rackspace-cloudservers-uk"
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
	 * This will delete all servers that start with {@link SERVER_NAME}
	 */
	private void deleteServer() {
		System.out.println("Delete Server");

		ServerApi serverApi = nova.getApi().getServerApiForZone(ZONE);
		FluentIterable<? extends Server> servers = serverApi.listInDetail().concat();
		
		for (Server server: servers) {
			if (server.getName().startsWith(SERVER_NAME)) {
				serverApi.delete(server.getId());
				System.out.println("  " + server);
			}
		}
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
