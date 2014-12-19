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
package org.jclouds.examples.rackspace.cloudfiles;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import org.jclouds.ContextBuilder;
import org.jclouds.openstack.swift.v1.domain.SwiftObject;
import org.jclouds.openstack.swift.v1.features.ObjectApi;
import org.jclouds.rackspace.cloudfiles.v1.CloudFilesApi;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.jclouds.examples.rackspace.cloudfiles.Constants.CONTAINER;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.PROVIDER;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.REGION;

/**
 * Gets an object from a container and displays the results.
 *
 * NOTE: Run the {@link UploadObjects} example prior to running this example.
 *
 */
public class GetObject implements Closeable {
   private final CloudFilesApi cloudFiles;

   /**
    * To get a username and API key see http://jclouds.apache.org/guides/rackspace/
    *
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      GetObject getObject = new GetObject(args[0], args[1]);

      try {
         SwiftObject swiftObject = getObject.getObject();
         getObject.writeObject(swiftObject);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         getObject.close();
      }
   }

   public GetObject(String username, String apiKey) {
      cloudFiles = ContextBuilder.newBuilder(PROVIDER)
            .credentials(username, apiKey)
            .buildApi(CloudFilesApi.class);

   }

   private SwiftObject getObject() {
      System.out.format("Get Object%n");

      ObjectApi objectApi = cloudFiles.getObjectApi(REGION, CONTAINER);
      SwiftObject swiftObject = objectApi.get("uploadObjectFromFile.txt");

      System.out.format("  %s%n", swiftObject);

      return swiftObject;
   }

   private void writeObject(SwiftObject swiftObject) throws IOException {
      System.out.format("Write Object%n");

      InputStream inputStream = swiftObject.getPayload().openStream();
      File file = File.createTempFile("uploadObjectFromFile", ".txt");
      BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));

      try {
         ByteStreams.copy(inputStream, outputStream);
      }
      finally {
         inputStream.close();
         outputStream.close();
      }

      System.out.format("  %s%n", file.getAbsolutePath());
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(cloudFiles, true);
   }
}
