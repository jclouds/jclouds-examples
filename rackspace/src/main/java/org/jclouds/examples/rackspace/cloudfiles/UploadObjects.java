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
package org.jclouds.examples.rackspace.cloudfiles;

import static com.google.common.io.Closeables.closeQuietly;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.openstack.swift.CommonSwiftAsyncClient;
import org.jclouds.openstack.swift.CommonSwiftClient;
import org.jclouds.openstack.swift.domain.SwiftObject;
import org.jclouds.rest.RestContext;

/**
 * Upload objects in the object storage container from the CreateContainer example.
 *  
 * @author Everett Toews
 */
public class UploadObjects implements Closeable {
   private BlobStore storage;
   private RestContext<CommonSwiftClient, CommonSwiftAsyncClient> swift;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      UploadObjects uploadContainer = new UploadObjects();

      try {
         uploadContainer.init(args);
         uploadContainer.uploadObjectFromFile();
         uploadContainer.uploadObjectFromString();
         uploadContainer.uploadObjectFromStringWithMetadata();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      finally {
         uploadContainer.close();
      }
   }

   private void init(String[] args) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the provider to "cloudfiles-uk"
      String provider = "cloudfiles-us";

      String username = args[0];
      String apiKey = args[1];

      BlobStoreContext context = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .buildView(BlobStoreContext.class);
      storage = context.getBlobStore();
      swift = context.unwrap();
   }

   /**
    * Upload an object from a File using the Swift API. 
    */
   private void uploadObjectFromFile() throws IOException {
      System.out.println("Upload Object From File");

      String filename = "uploadObjectFromFile";
      String suffix = ".txt";

      File tempFile = File.createTempFile(filename, suffix);
      tempFile.deleteOnExit();

      BufferedWriter out = new BufferedWriter(new FileWriter(tempFile));
      out.write("uploadObjectFromFile");
      out.close();

      SwiftObject object = swift.getApi().newSwiftObject();
      object.getInfo().setName(filename + suffix);
      object.setPayload(tempFile);

      swift.getApi().putObject(Constants.CONTAINER, object);

      System.out.println("  " + filename + suffix);
   }

   /**
    * Upload an object from a String using the Swift API. 
    */
   private void uploadObjectFromString() {
      System.out.println("Upload Object From String");

      String filename = "uploadObjectFromString.txt";

      SwiftObject object = swift.getApi().newSwiftObject();
      object.getInfo().setName(filename);
      object.setPayload("uploadObjectFromString");

      swift.getApi().putObject(Constants.CONTAINER, object);

      System.out.println("  " + filename);
   }

   /**
    * Upload an object from a String with metadata using the BlobStore API. 
    */
   private void uploadObjectFromStringWithMetadata() {
      System.out.println("Upload Object From String With Metadata");

      String filename = "uploadObjectFromStringWithMetadata.txt";

      Map<String, String> userMetadata = new HashMap<String, String>();
      userMetadata.put("key1", "value1");

      Blob blob = storage.blobBuilder(filename)
            .payload("uploadObjectFromStringWithMetadata")
            .userMetadata(userMetadata)
            .build();

      storage.putBlob(Constants.CONTAINER, blob);

      System.out.println("  " + filename);
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(storage.getContext());
   }
}
