/*
 * Copyright (C) 2011 Cloud Conscious, LLC. <info@cloudconscious.com>
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

package org.jclouds.examples.blobstore.osgi.shell;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

/**
 * @author: iocanel
 */
@Command(scope = "jclouds", name = "blobstore-write", description = "Writes data from the blobstore")
public class WriteCommand extends BlobStoreCommandSupport {

    @Argument(index = 0, name = "bucketName", description = "The name of the bucket", required = true, multiValued = false)
    String bucketName;

    @Argument(index = 1, name = "blobName", description = "The name of the blob", required = true, multiValued = false)
    String blobName;

    @Argument(index = 2, name = "payload", description = "The payload", required = true, multiValued = false)
    String payload;


    @Override
    protected Object doExecute() throws Exception {
        if(blobStoreService != null) {
            blobStoreService.write(bucketName,blobName,payload);
        } else {
            System.err.println("No blob store service configured.");
        }
        return null;
    }
}
