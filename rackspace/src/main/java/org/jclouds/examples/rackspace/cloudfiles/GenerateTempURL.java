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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpResponse;
import org.jclouds.http.HttpResponseException;
import org.jclouds.util.Strings2;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * The Temporary URL feature (TempURL) allows you to create limited-time Internet addresses which allow you to grant 
 * limited access to your Cloud Files account. Using TempURL, you may allow others to retrieve or place objects in 
 * your Cloud Files account for as long or as short a time as you wish. Access to the TempURL is independent of 
 * whether or not your account is CDN-enabled. And even if you don't CDN-enable a directory, you can still grant 
 * temporary public access through a TempURL.
 * 
 * This feature is useful if you want to allow a limited audience to download a file from your Cloud Files account or
 * website. You can give out the TempURL and know that after a specified time, no one will be able to access your 
 * object through the address. Or, if you want to allow your audience to upload objects into your Cloud Files account, 
 * you can give them a TempURL. After the specified time expires, no one will be able to upload to the address.
 * 
 * Additionally, you need not worry about time running out when someone downloads a large object. If the time expires 
 * while a file is being retrieved, the download will continue until it is finished. Only the link will expire.
 *  
 * @author Everett Toews
 */
public class GenerateTempURL implements Closeable {
   private static final String FILENAME = "object.txt";
   private static final int TEN_MINUTES = 10 * 60;
   
   private BlobStore storage;
   private BlobStoreContext storageContext;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      GenerateTempURL generateTempURL = new GenerateTempURL();

      try {
         generateTempURL.init(args);
         generateTempURL.generatePutTempURL();
         generateTempURL.generateGetTempURL();
         generateTempURL.generateDeleteTempURL();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         generateTempURL.close();
      }
   }

   private void init(String[] args) {
      // The provider configures jclouds To use the Rackspace Cloud (US)
      // To use the Rackspace Cloud (UK) set the provider to "cloudfiles-uk"
      String provider = "cloudfiles-us";

      String username = args[0];
      String apiKey = args[1];

      storageContext = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .buildView(BlobStoreContext.class);
      storage = storageContext.getBlobStore();
   }
   
   private void generatePutTempURL() throws IOException {
      System.out.println("Generate PUT Temp URL");

      String payload = "This object will be public for 10 minutes.";
      Blob blob = storage.blobBuilder(FILENAME).payload(payload).contentType("text/plain").build();
      HttpRequest request = storageContext.getSigner().signPutBlob(Constants.CONTAINER, blob, TEN_MINUTES);
      
      System.out.println("  " + request.getMethod() + " " + request.getEndpoint());
      
      // PUT the file using jclouds
      HttpResponse response = storageContext.utils().http().invoke(request);
      int statusCode = response.getStatusCode();
      
      if (statusCode >= 200 && statusCode < 299) {
         System.out.println("  PUT Success (" + statusCode + ")");
      }
      else {
         throw new HttpResponseException(null, response);
      }
   }

   private void generateGetTempURL() throws IOException {
      System.out.println("Generate GET Temp URL");
      
      HttpRequest request = storageContext.getSigner().signGetBlob(Constants.CONTAINER, FILENAME, TEN_MINUTES);
      
      System.out.println("  " + request.getMethod() + " " + request.getEndpoint());
      
      // GET the file using jclouds
      File file = File.createTempFile(FILENAME, ".tmp");
      String content = Strings2.toString(storageContext.utils().http().invoke(request).getPayload());
      Files.write(content, file, Charsets.UTF_8);
      
      System.out.println("  GET Success (" + file.getAbsolutePath() + ")");
   }

   private void generateDeleteTempURL() throws IOException {
      System.out.println("Generate DELETE Temp URL");
      
      HttpRequest request = storageContext.getSigner().signRemoveBlob(Constants.CONTAINER, FILENAME);
      
      System.out.println("  " + request.getMethod() + " " + request.getEndpoint());
      
      // DELETE the file using jclouds
      HttpResponse response = storageContext.utils().http().invoke(request);
      int statusCode = response.getStatusCode();
      
      if (statusCode >= 200 && statusCode < 299) {
         System.out.println("  DELETE Success (" + statusCode + ")");
      }
      else {
         throw new HttpResponseException(null, response);
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(storage.getContext());
   }
}
