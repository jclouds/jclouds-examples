# compute-basics

This is a simple example command line client that creates a node in a [ComputeService](http://code.google.com/p/jclouds/wiki/ComputeGuide) provider and executes "echo hello" on everything in its group.

## Build

Ensure you have maven 3.02 or higher installed, then execute 'mvn install' to build the example.  Note you also need an ssh key setup in your home directory.

## Run

Invoke the jar, passing the name of the cloud provider you with to access (ex. aws-ec2, gogrid), identity (ex. accesskey, username), credential (ex. secretkey, password), then the name of the group you'd like to add the node to. The 4th parameter must be add, exec or destroy, noting that destroy will destroy all nodes in the group. If the 4th parameter is exec, you must quote a command to execute across the group of nodes.

java -jar target/compute-basics-jar-with-dependencies.jar provider identity credential mygroup add

java -jar target/compute-basics-jar-with-dependencies.jar provider identity credential mygroup add

java -jar target/compute-basics-jar-with-dependencies.jar provider identity credential mygroup exec "echo hello"

java -jar target/compute-basics-jar-with-dependencies.jar provider identity credential mygroup destroy

Ex. for GoGrid

java -jar target/compute-basics-jar-with-dependencies.jar gogrid apikey sharedsecret mygroup add

Ex. for Amazon EC2

java -jar target/compute-basics-jar-with-dependencies.jar aws-ec2 accesskey secretkey mygroup add

## License

Copyright (C) 2011 Cloud Conscious, LLC. <info@cloudconscious.com>

Licensed under the Apache License, Version 2.0 
