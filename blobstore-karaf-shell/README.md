# blobstore-karaf-shell

This is a simple examples that demonstrates the using a [BlobStore](http://jclouds.apache.org/start/blobstore/) provider from within [Apache Karaf](http://karaf.apache.org/) Runtime.

This example adds two osgi shell commands, that read and write to a blobstore. The context of the blobstore can be configured via OSGi Configuration Admin, which allows the switching blobstore contexts (providers, keys etc) "on the fly". 
## Build

The sample builds with maven 2.2.1 or higher (however its tested with 3.0.3). Execute 'mvn install' to build the example.

## Run
From within Apache Karaf (2.2.0 or higher) type:
karaf@root>osgi:install -s mvn:org.jclouds.examples/blobstore-karaf-shell/1.0-SNAPSHOT

Once the sample is install, create a new configuration named "org.jclouds.blobstore" and add the provider,the access key id and the secret key. 

karaf@root>config:edit org.jclouds.blobstore

karaf@root>config:propset provider aws-s3

karaf@root>config:propset accessKeyId XXXXXX

karaf@root>config:propset secretKey XXXXXX

karaf@root>config:update

An alternative is to create a cfg file with the key values listed above and throw it under karaf/etc.

Now you can use the shell commands, for example:
karaf@root>jclouds:blobstore-write mybucket myblob JCloudsRocks!
karaf@root>jclouds:blobstore-read mybucket myblob
JCloudsRocks!

## License

Copyright (C) 2009-2014 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 
