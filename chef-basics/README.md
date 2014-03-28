# chef-basics

This is a simple example command line client that creates a node in a [ComputeService](http://jclouds.apache.org/start/compute/) provider and installs an Apache web server on everything in its group using Chef.

## Build

Ensure you have maven 3.02 or higher installed, then execute 'mvn install' to build the example.  Note you also need an ssh key setup in your home directory.

If you don't already have ~/.ssh/id_rsa present, generate a key with the command 'ssh-keygen -t rsa' and leave the passphrase blank.

Also make sure you have the private keys for the client and validator if you are using a Chef Server.

## Run

Invoke the jar, passing the name of the cloud provider you with to access (ex. aws-ec2, gogrid), identity (ex. accesskey, username), credential (ex. secretkey, password), then the name of the group you'd like to add the node to. The 5th parameter must be add, chef or destroy, noting that destroy will destroy all nodes in the group. If the 5th parameter is chef or solo, you must provide the list of recipes to install, separated by commas.

Also, if the 5th parameter is 'chef', you must provide the connection details to the chef server. See the examples below:

java -jar target/chef-basics-jar-with-dependencies.jar provider identity credential mygroup add

java -Dchef.endpoint=http://chefendpoint -Dchef.client=clientname -Dchef.validator=validatorname -jar target/chef-basics-jar-with-dependencies.jar provider identity credential mygroup chef apache2

java -jar target/chef-basics-jar-with-dependencies.jar provider identity credential mygroup solo apache2

java -jar target/chef-basics-jar-with-dependencies.jar provider identity credential mygroup destroy

Ex. for Amazon EC2

java -jar target/chef-basics-jar-with-dependencies.jar aws-ec2 accesskey secretkey mygroup add

Ex. for HP Cloud

java -jar target/chef-basics-jar-with-dependencies.jar hpcloud-compute tenantId:accesskey secretkey mygroup add

Ex. for TryStack.org

java -jar target/chef-basics-jar-with-dependencies.jar trystack-nova tenantId:user password mygroup add

Ex. for Abiquo

java -Dabiquo.endpoint=http://abiquohost/api -jar target/chef-basics-jar-with-dependencies.jar abiquo user password mygroup add

Ex. for your own OpenStack Nova

java -Dopenstack-nova.image-id=RegionOne/15 -Dopenstack-nova.login-user=ubuntu -Djclouds.trust-all-certs=true -Djclouds.keystone.credential-type=passwordCredentials -Dopenstack-nova.endpoint=https://keystone:35357 -jar target/chef-basics-jar-with-dependencies.jar openstack-nova tenantId:user password mygroup add

Ex. for Virtualbox
java -jar target/chef-basics-jar-with-dependencies.jar virtualbox vboxwebsrv-username vboxwebsrv-password mygroup add

Ex. for your own OpenStack Nova emulating EC2

java -Dopenstack-nova-ec2.image-id=nova/ami-00000009 -Dopenstack-nova-ec2.login-user=ubuntu -Djclouds.trust-all-certs=true -Dopenstack-nova-ec2.endpoint=https://novahost/services/Cloud -jar target/chef-basics-jar-with-dependencies.jar openstack-nova-ec2 tenantId:accesskey secretkey mygroup add

## License

Copyright (C) 2009-2014 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 
