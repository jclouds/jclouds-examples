# blobstore-largeblob

This is a simple example command line client that creates a container in a [BlobStore](http://jclouds.apache.org/start/blobstore/) provider and uploads a large file.

## Build

Ensure you have maven 3.02 or higher installed, then execute 'mvn install' to build the example.

## Run

Invoke the jar, passing the name of the cloud provider you with to access (aws-s3 is currently tested), identity (ex. accesskey, username), credential (ex. secretkey, password), the filename you want to upload, then the name of the container you'd like to create, then the object name and eventually the optional parameters plainhttp or securehttp and a number representing the number of threads.

Ex. for Amazon S3

java -jar target/blobstore-largeblob-jar-with-dependencies.jar aws-s3 accesskey secretkey inputfile myfavoritecontainer keyname plainhttp 5

"plainhttp" "5" are the two optional parameters. Default values are securehttp and 4 threads.

The inputfile size has to be at least 32Mbytes size to be used multipart upload. Below this size it will fall back to simple upload. 

## License

Copyright (C) 2009-2014 The Apache Software Foundation

Licensed under the Apache License, Version 2.0
