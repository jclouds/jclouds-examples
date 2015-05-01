# compute-basics

This is a simple example command line client that creates a node in a [ComputeService](http://jclouds.apache.org/start/compute/) provider and executes `echo hello` on everything in its group.

## Build

Ensure you have **maven 3.02** or higher installed, then execute `mvn install` to build the example. 
Note you also need an ssh key setup in your home directory.

If you don't already have `~/.ssh/id_rsa` present, generate a key with the command `ssh-keygen -t rsa` and leave the passphrase blank.

## Run

Invoke the jar, passing the name of the cloud provider you with to access (ex. aws-ec2, gogrid), identity (ex. accesskey, username), credential (ex. secretkey, password), then the name of the group you'd like to add the node to. 

The 4th parameter represents the **action** you want to run. Supported actions are:

- add
- exec
- run
- destroy
- listnodes
- listimages

Noting that **destroy** will destroy all nodes in the group. If the 4th parameter is exec, you must quote a command to execute across the group of nodes.

To add a node to the group *mygroup*:

    java -jar target/compute-basics-jar-with-dependencies.jar provider identity credential mygroup add
    
To execute a command to all nodes of the group *mygroup*:

    java -jar target/compute-basics-jar-with-dependencies.jar provider identity credential mygroup exec "echo hello"

To run a script file in all nodes of the group *mygroup*

    java -jar target/compute-basics-jar-with-dependencies.jar provider identity credential mygroup run /path/to/script-file

To destroy all nodes of the group *mygroup*:

    java -jar target/compute-basics-jar-with-dependencies.jar provider identity credential mygroup destroy

To list all nodes (the *groupname* parameter is not used):

    java -jar target/compute-basics-jar-with-dependencies.jar provider identity credential mygroup listnodes

To list all images (the *groupname* parameter is not used):

    java -jar target/compute-basics-jar-with-dependencies.jar provider identity credential mygroup listimages


### Amazon EC2

    java -jar target/compute-basics-jar-with-dependencies.jar aws-ec2 \
          accesskey secretkey mygroup add

### HP Cloud

    java -jar target/compute-basics-jar-with-dependencies.jar hpcloud-compute \
          tenantId:accesskey secretkey mygroup add

### TryStack.org

    java -jar target/compute-basics-jar-with-dependencies.jar trystack-nova tenantId:user password mygroup add

### Your own OpenStack Nova

    java \
         -Dopenstack-nova.image-id=RegionOne/15 \
         -Dopenstack-nova.login-user=ubuntu \
         -Djclouds.trust-all-certs=true \
         -Djclouds.keystone.credential-type=passwordCredentials \
         -Dopenstack-nova.endpoint=https://keystone:35357 \
         -jar target/compute-basics-jar-with-dependencies.jar \
         openstack-nova tenantId:user password mygroup add

### Your own OpenStack Nova emulating EC2

    java \
         -Dopenstack-nova-ec2.image-id=nova/ami-00000009 \
         -Dopenstack-nova-ec2.login-user=ubuntu \
         -Djclouds.trust-all-certs=true \
         -Dopenstack-nova-ec2.endpoint=https://novahost/services/Cloud \
         -jar target/compute-basics-jar-with-dependencies.jar \
         openstack-nova-ec2 tenantId:accesskey secretkey mygroup add

### Google Compute Engine provider

    java -jar target/compute-basics-jar-with-dependencies.jar \
         google-compute-engine \
         your-project-service-account-email@developer.gserviceaccount.com \
         /path/to/json-key.json \
         mygroup add

For more information on service accounts and getting a JSON key see [here](https://developers.google.com/console/help/new/#serviceaccounts)

## License

Copyright (C) 2009-2014 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 
