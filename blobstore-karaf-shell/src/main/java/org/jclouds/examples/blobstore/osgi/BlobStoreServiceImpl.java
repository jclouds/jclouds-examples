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
package org.jclouds.examples.blobstore.osgi;

import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.blobstore.domain.Blob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * @author: iocanel
 */
public class BlobStoreServiceImpl implements BlobStoreService {

    private static final Logger logger = LoggerFactory.getLogger(BlobStoreServiceImpl.class);

    private String accessKeyId;
    private String secretKey;
    private String provider;

    private BlobStoreContext context;

    /**
     * Constructor
     */
    public BlobStoreServiceImpl() {

    }

    public Object read(String bucket, String blobName) {
        Object result = null;
        ObjectInputStream ois = null;
        context = new BlobStoreContextFactory().createContext(provider, accessKeyId, secretKey);
        if (context != null) {
            BlobStore blobStore = context.getBlobStore();
            blobStore.createContainerInLocation(null, bucket);

            InputStream is = blobStore.getBlob(bucket, blobName).getPayload().getInput();

            try {
                ois = new ObjectInputStream(is);
                result = ois.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException e) {
                    }
                }

                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
        } else logger.warn("Blob store context is null.");
        return result;
    }


    public void write(String bucket, String blobName, Object object) {
        context = new BlobStoreContextFactory().createContext(provider, accessKeyId, secretKey);
        if (context != null) {
            BlobStore blobStore = context.getBlobStore();
            Blob blob = blobStore.blobBuilder(blobName).build();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = null;

            try {
                oos = new ObjectOutputStream(baos);
                oos.writeObject(object);
                blob.setPayload(baos.toByteArray());
                blobStore.putBlob(bucket, blob);
            } catch (IOException e) {
                logger.error("Error while writing blob", e);
            } finally {
                if (oos != null) {
                    try {
                        oos.close();
                    } catch (IOException e) {
                    }
                }

                if (baos != null) {
                    try {
                        baos.close();
                    } catch (IOException e) {
                    }
                }
            }
        } else logger.warn("Blob store context is null.");
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
