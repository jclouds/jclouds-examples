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

package org.jclouds.examples.blobstore;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class BlobUploaderMain {
   private static int numThreads = 3;
   public static long totalBytes = 0;
   public static AtomicLong bytesUploaded = new AtomicLong(0);

   public static void main(String[] args) throws IOException {

      OptionParser parser = new OptionParser();
      parser.accepts("directory").withRequiredArg().required().ofType(String.class);
      parser.accepts("provider").withRequiredArg().required().ofType(String.class);
      parser.accepts("username").withRequiredArg().required().ofType(String.class);
      parser.accepts("password").withRequiredArg().required().ofType(String.class);
      parser.accepts("region").withRequiredArg().required().ofType(String.class);
      parser.accepts("threads").withRequiredArg().ofType(Integer.TYPE).describedAs("number of parallel threads");
      OptionSet options = null;

      try {
         options = parser.parse(args);
      } catch (OptionException e) {
         System.out.println(e.getLocalizedMessage());
         parser.printHelpOn(System.out);
         return;
      }

      if (options.has("threads")) {
         numThreads = Integer.valueOf((String)options.valueOf("numThreads"));
      }

      File rootDir = new File((String) options.valueOf("directory"));
      Collection<File> files = FileUtils.listFiles(rootDir, CanReadFileFilter.CAN_READ, TrueFileFilter.TRUE);
      totalBytes = FileUtils.sizeOfDirectory(rootDir);

      System.out.println("Uploading " + rootDir.getName() + " " + totalBytes / FileUtils.ONE_MB + "MB");

      ExecutorService executor = Executors.newFixedThreadPool(numThreads);

      for (File f : files) {
         BlobUploader b =
         new BlobUploader(
               (String) options.valueOf("username"),
               (String) options.valueOf("password"),
               (String) options.valueOf("provider"),
               (String) options.valueOf("region"),
               f);
         executor.execute(b);
      }
      executor.shutdown();

      try {
         executor.awaitTermination(1, TimeUnit.DAYS);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }
}
