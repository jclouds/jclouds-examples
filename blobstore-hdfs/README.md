# blobstore-hdfs

This is a simple example which shows how we can extends the Payload to have HDFS for using with Blobstore.
Then the application layer is using an upload from HDFS.
The reason why such kind of HDFS payload currently is considered only an example application is that
people are using different versions of Hadoop, thus the adequate version needs to be matched exactly
which is not a simple decision in general case. As an example, it is the simplest way to present it.

Note that, if you run this example and you will see an exception showing incompatible version between 
your hdfs client and the running server's hdfs version, then switch the version of hadoop-core from
0.20.2-cdh3u0 to your used version. 

## Build

Ensure you have maven 3.02 or higher installed, then execute 'mvn install' to build the example.

## Run

First of all, you need some Hadoop running in distributed or pseudo distributed mode.
The easiest way to test it, just install it a CDH3 version of Cloudera Hadoop as it is
described here: https://ccp.cloudera.com/display/CDHDOC/CDH3+Deployment+in+Pseudo-Distributed+Mode 

Invoke the jar, passing the name of the cloud provider you with to access (aws-s3 is currently tested), identity (ex. accesskey, username), credential (ex. secretkey, password), the filename you want to upload from hfds (for example hdfs://localhost:8020/user/yourusername/yourfile), then the name of the container you'd like to create, then the object name and eventually the optional parameters plainhttp or securehttp and a number representing the number of threads.

Ex. for Amazon S3

"plainhttp" "5" are the two optional parameters. Default values are securehttp and 4 threads.

The hdfs input file size has to be at least 32Mbytes size to be used multipart upload. Below this size it will fall back to simple upload. 

## License

Copyright (C) 2009-2014 The Apache Software Foundation

Licensed under the Apache License, Version 2.0
