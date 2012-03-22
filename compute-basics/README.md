# compute-basics

This is a simple example command line client that creates a node in a [ComputeService](http://code.google.com/p/jclouds/wiki/ComputeGuide) provider and executes "echo hello" on everything in its group.

## Build

Ensure you have maven 3.02 or higher installed, then execute 'mvn install' to build the example.  Note you also need an ssh key setup in your home directory.

If you don't already have ~/.ssh/id_rsa present, generate a key with the command 'ssh-keygen -t rsa' and leave the passphrase blank.

## Run

Invoke the jar, passing the name of the cloud provider you with to access (ex. aws-ec2, gogrid), identity (ex. accesskey, username), credential (ex. secretkey, password), then the name of the group you'd like to add the node to. The 4th parameter must be add, exec or destroy, noting that destroy will destroy all nodes in the group. If the 4th parameter is exec, you must quote a command to execute across the group of nodes.

java -jar target/compute-basics-jar-with-dependencies.jar provider identity credential mygroup add

java -jar target/compute-basics-jar-with-dependencies.jar provider identity credential mygroup add

java -jar target/compute-basics-jar-with-dependencies.jar provider identity credential mygroup exec "echo hello"

java -jar target/compute-basics-jar-with-dependencies.jar provider identity credential mygroup destroy

Ex. for OpenStack Nova

java -Dopenstack-nova.endpoint=https://keystone:35357 -jar target/compute-basics-jar-with-dependencies.jar openstack-nova tenantId:accesskey secretKey mygroup add

Ex. for Amazon EC2

java -jar target/compute-basics-jar-with-dependencies.jar aws-ec2 accesskey secretkey mygroup add

## License

Copyright (C) 2009-2012 jclouds, Inc.

Licensed under the Apache License, Version 2.0 
