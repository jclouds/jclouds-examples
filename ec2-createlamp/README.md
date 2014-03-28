# ec2-createlamp

This is a simple example command line client that creates a lamp server and everything you need to do that in [EC2](http://code.google.com/p/jclouds/wiki/EC2)

## Build

Ensure you have maven 3.02 or higher installed, then execute 'mvn install' to build the sample.

## Run

Invoke the jar, passing your aws credentials and the name you wish to create or destroy

### Creating your Instance

The create command will create a keypair, security group, and an instance.  It also blocks until the web server is running.

java -jar target/ec2-createlamp-jar-with-dependencies.jar accesskey secretkey create adrianalmighty

### Destroying your Instance

The destroy command will clear up the instance, key, and security group.

java -jar target/ec2-createlamp-jar-with-dependencies.jar accesskey secretkey destroy adrianalmighty

## License

Copyright (C) 2009-2014 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 
