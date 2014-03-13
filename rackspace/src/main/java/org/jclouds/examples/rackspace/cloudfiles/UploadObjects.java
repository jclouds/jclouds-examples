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

import static org.jclouds.examples.rackspace.cloudfiles.Constants.CONTAINER;
import static org.jclouds.examples.rackspace.cloudfiles.Constants.PROVIDER;

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
import org.jclouds.cloudfiles.CloudFilesClient;
import org.jclouds.openstack.swift.CommonSwiftClient;
import org.jclouds.openstack.swift.domain.SwiftObject;

import com.google.common.io.Closeables;

/**
 * Upload objects in the Cloud Files container from the CreateContainer example.
 *  
 * @author Everett Toews
 */
public class UploadObjects implements Closeable {
   private final BlobStore blobStore;
   private final CommonSwiftClient swift;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) throws IOException {
      UploadObjects uploadContainer = new UploadObjects(args[0], args[1]);

      try {
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

   public UploadObjects(String username, String apiKey) {
      ContextBuilder builder = ContextBuilder.newBuilder(PROVIDER)
                                  .credentials(username, apiKey);
      blobStore = builder.buildView(BlobStoreContext.class).getBlobStore();
      swift = builder.buildApi(CloudFilesClient.class);
   }

   /**
    * Upload an object from a File using the Swift API. 
    */
   private void uploadObjectFromFile() throws IOException {
      System.out.format("Upload Object From File%n");

      String filename = "uploadObjectFromFile";
      String suffix = ".txt";

      File tempFile = File.createTempFile(filename, suffix);
      tempFile.deleteOnExit();

      BufferedWriter out = new BufferedWriter(new FileWriter(tempFile));
      out.write("uploadObjectFromFile");
      out.close();

      SwiftObject object = swift.newSwiftObject();
      object.getInfo().setName(filename + suffix);
      object.setPayload(tempFile);

      swift.putObject(CONTAINER, object);

      System.out.format("  %s%s%n", filename, suffix);
   }

   /**
    * Upload an object from a String using the Swift API. 
    */
   private void uploadObjectFromString() {
      System.out.format("Upload Object From String%n");

      String filename = "uploadObjectFromString.txt";

      SwiftObject object = swift.newSwiftObject();
      object.getInfo().setName(filename);
      object.setPayload("uploadObjectFromString");

      swift.putObject(CONTAINER, object);

      System.out.format("  %s%n", filename);
   }

   /**
    * Upload an object from a String with metadata using the BlobStore API. 
    */
   private void uploadObjectFromStringWithMetadata() {
      System.out.format("Upload Object From String With Metadata%n");

      String filename = "uploadObjectFromStringWithMetadata.txt";

      Map<String, String> userMetadata = new HashMap<String, String>();
      userMetadata.put("key1", "value1");

      Blob blob = blobStore.blobBuilder(filename)
            .payload("uploadObjectFromStringWithMetadata")
            .userMetadata(userMetadata)
            .build();

      blobStore.putBlob(CONTAINER, blob);

      System.out.format("  %s%n", filename);
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() throws IOException {
      Closeables.close(blobStore.getContext(), true);
      Closeables.close(swift, true);
   }
}
