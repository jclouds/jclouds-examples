(ns create-list.containers
  (:use org.jclouds.blobstore2))

(defn create-and-list
  "create a container, then list all containers in your account"
  ([^String provider ^String provider-identity ^String provider-credential ^String container-to-create]
    (create-and-list (blobstore provider provider-identity provider-credential) container-to-create))
  ([^BlobStore blobstore container-to-create]
    (create-container blobstore container-to-create)
    (containers blobstore))) 

