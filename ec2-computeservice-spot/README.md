# ec2-computeservice-spot

This is a simple example command line client that creates a spot instance in [EC2](http://code.google.com/p/jclouds/wiki/EC2) using the ComputeService interface.

## Build

Ensure you have maven 3.02 or higher installed, then execute 'mvn install' to build the sample.

Note you'll also need to ensure you have an ssh key in your home directory.

## Run

Invoke the jar, passing your aws credentials and the name you wish to create or destroy

### Creating your Instance

The create command will create a keypair, security group, and an instance in running state.

java -jar target/ec2-computeservice-spot-jar-with-dependencies.jar accesskey secretkey groupname create

### Destroying your Instance

The destroy command will clear up the instance, key, and security group.

java -jar target/ec2-computeservice-spot-jar-with-dependencies.jar accesskey secretkey groupname destroy

## License

Copyright (C) 2009-2014 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 
