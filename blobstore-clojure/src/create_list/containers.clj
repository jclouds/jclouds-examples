;
; Licensed to the Apache Software Foundation (ASF) under one or more
; contributor license agreements.  See the NOTICE file distributed with
; this work for additional information regarding copyright ownership.
; The ASF licenses this file to You under the Apache License, Version 2.0
; (the "License"); you may not use this file except in compliance with
; the License.  You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;

(ns create-list.containers
  (:use org.jclouds.blobstore2)
  (:import org.jclouds.blobstore.BlobStore))

(defn create-and-list
  "create a container, then list all containers in your account"
  ([^String provider ^String provider-identity ^String provider-credential ^String container-to-create]
    (create-and-list (blobstore provider provider-identity provider-credential) container-to-create))
  ([^BlobStore blobstore container-to-create]
    (create-container blobstore container-to-create)
    (containers blobstore))) 

