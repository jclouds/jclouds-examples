# blobstore-basics

This is a simple example command line client that creates a container in a BlobStore provider and lists the size of all the other containers.

## Build

Ensure you have maven 3.02 or higher installed, then execute 'mvn install' to build the example.

## Run

Invoke the jar, passing the name of the cloud provider you with to access (ex. aws-s3, googlestorage), identity (ex. accesskey, username), credential (ex. secretkey, password), then the name of the container you'd like to create.

Ex. for Amazon S3

java -jar target/blobstore-basics-jar-with-dependencies.jar aws-s3 accesskey secretkey myfavoritecontainer

Ex. for Rackspace CloudFiles

java -jar target/blobstore-basics-jar-with-dependencies.jar cloudfiles-us username apikey myfavoritecontainer


## License

Copyright (C) 2011 Cloud Conscious, LLC. <info@cloudconscious.com>

Licensed under the Apache License, Version 2.0 
