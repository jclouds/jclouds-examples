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
package org.jclouds.examples.blobstore.osgi.shell;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.jclouds.examples.blobstore.osgi.BlobStoreService;

/**
 * @author: iocanel
 */
public abstract class BlobStoreCommandSupport extends OsgiCommandSupport {

    protected BlobStoreService blobStoreService;

    public BlobStoreService getBlobStoreService() {
        return blobStoreService;
    }

    public void setBlobStoreService(BlobStoreService service) {
        this.blobStoreService = service;
    }
}
