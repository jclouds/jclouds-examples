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
package org.jclouds.examples.rackspace;

import org.jclouds.examples.rackspace.cloudfiles.CloudFilesPublish;
import org.jclouds.examples.rackspace.cloudfiles.CreateContainer;
import org.jclouds.examples.rackspace.cloudfiles.CreateObjects;
import org.jclouds.examples.rackspace.cloudfiles.DeleteObjectsAndContainer;
import org.jclouds.examples.rackspace.cloudfiles.ListContainers;
import org.jclouds.examples.rackspace.cloudfiles.ListObjects;
import org.jclouds.examples.rackspace.cloudservers.CloudServersPublish;
import org.jclouds.examples.rackspace.cloudservers.CreateServer;
import org.jclouds.examples.rackspace.cloudservers.DeleteServer;
import org.jclouds.examples.rackspace.cloudservers.ListServersWithFiltering;
import org.jclouds.examples.rackspace.cloudservers.ServerMetadata;

/**
 * This example smoke tests all of the other examples in these packages.
 *  
 * @author Everett Toews
 */
public class SmokeTest {

	/**
	 * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
	 * 
	 * The first argument (args[0]) must be your username
	 * The second argument (args[1]) must be your API key
	 */
	public static void main(String[] args) {
		SmokeTest smokeTest = new SmokeTest();
		smokeTest.smokeTest(args);
	}

	private void smokeTest(String[] args) {
		Authentication.main(args);
		Logging.main(args);
		
		CloudServersPublish.main(args);
		CreateServer.main(args);
		ListServersWithFiltering.main(args);
		ServerMetadata.main(args);
		DeleteServer.main(args);
		
		CloudFilesPublish.main(args);
		CreateContainer.main(args);
		ListContainers.main(args);
		CreateObjects.main(args);
		ListObjects.main(args);
		DeleteObjectsAndContainer.main(args);
	}
}
