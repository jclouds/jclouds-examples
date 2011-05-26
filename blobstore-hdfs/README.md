# blobstore-hdfs

This is a simple example which shows how we can extends the Payload to have HDFS for using with Blobstore.
Then the application layer is using an upload from HDFS.
The reason why such kind of HDFS payload currently is considered only an example application is that
people are using different versions of Hadoop, thus the adequate version needs to be matched exactly
which is not a simple decision in general case. As an example, it is the simplest way to present it. 

## Build

Ensure you have maven 3.02 or higher installed, then execute 'mvn install' to build the example.

## Run

Invoke the jar, passing the name of the cloud provider you with to access (aws-s3 is currently tested), identity (ex. accesskey, username), credential (ex. secretkey, password), the filename you want to upload, then the name of the container you'd like to create, then the object name and eventually the optional parameters plainhttp or securehttp and a number representing the number of threads.

Ex. for Amazon S3


"plainhttp" "5" are the two optional parameters. Default values are securehttp and 4 threads.

The hdfs input file size has to be at least 32Mbytes size to be used multipart upload. Below this size it will fall back to simple upload. 

## License

Copyright (C) 2011 Cloud Conscious, LLC. <info@cloudconscious.com>

Licensed under the Apache License, Version 2.0 
