/**
 *
 * Copyright (C) 2010 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.jclouds.examples.blobstore.hdfs.io.payloads;

import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.jclouds.io.payloads.BasePayload;

import com.google.common.base.Throwables;

public class HdfsPayload extends BasePayload<Path> {

   private Configuration configuration;

   public HdfsPayload(final Path content, final Configuration configuration)
         throws IOException {
      this(content, configuration, content.getFileSystem(configuration)
            .getFileStatus(content).getLen());
   }

   public HdfsPayload(final Path content, final Configuration configuration,
         final long length) throws IOException {
      super(content);
      this.configuration = configuration;
      getContentMetadata().setContentLength(length);
   }

   public InputStream getInput() {
      try {
         return content.getFileSystem(configuration).open(content);
      } catch (IOException e) {
         Throwables.propagate(e);
         return null;
      }
   }

   public FileSystem getFileSystem() throws IOException {
      return content.getFileSystem(configuration);
   }

   public Configuration getConfiguration() {
      return configuration;
   }
}
