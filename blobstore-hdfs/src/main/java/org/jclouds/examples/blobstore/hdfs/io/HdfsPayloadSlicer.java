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

package org.jclouds.examples.blobstore.hdfs.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Singleton;

import org.apache.hadoop.fs.FSDataInputStream;
import org.jclouds.examples.blobstore.hdfs.io.payloads.HdfsPayload;
import org.jclouds.io.Payload;
import org.jclouds.io.internal.BasePayloadSlicer;
import org.jclouds.io.payloads.InputStreamSupplierPayload;

import com.google.common.io.Closeables;
import com.google.common.io.InputSupplier;
import com.google.common.io.LimitInputStream;

@Singleton
public class HdfsPayloadSlicer extends BasePayloadSlicer {

   @Override
   public Payload slice(Payload input, long offset, long length) {
      checkNotNull(input);
      checkArgument(offset >= 0, "offset is negative");
      checkArgument(length >= 0, "length is negative");
      Payload returnVal;
      if (input instanceof HdfsPayload) {
         returnVal = doSlice(
               (FSDataInputStream) ((HdfsPayload) input).getInput(), offset,
               length);
         return copyMetadataAndSetLength(input, returnVal, length);
      } else {
         return super.slice(input, offset, length);
      }
   }

   protected Payload doSlice(final FSDataInputStream inputStream,
         final long offset, final long length) {
      return new InputStreamSupplierPayload(new InputSupplier<InputStream>() {
         public InputStream getInput() throws IOException {
            if (offset > 0) {
               try {
                  inputStream.seek(offset);
               } catch (IOException e) {
                  Closeables.closeQuietly(inputStream);
                  throw e;
               }
            }
            return new LimitInputStream(inputStream, length);
         }
      });
   }
}
