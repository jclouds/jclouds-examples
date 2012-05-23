# minecraft-compute

This is a simple example command line client that creates a node in a [ComputeService](http://code.google.com/p/jclouds/wiki/ComputeGuide) provider and starts a [Minecraft](http://www.minecraft.net/) server on it.

Note there are handy commands including add, list, pids, and destroy.

## Build

Ensure you have maven 3.02 or higher installed, then execute 'mvn install' to build the example.  Note you also need an ssh key setup in your home directory.

If you don't already have ~/.ssh/id_rsa present, generate a key with the command 'ssh-keygen -t rsa' and leave the passphrase blank.

## Run

Invoke the jar, passing the name of the cloud provider you with to access (ex. aws-ec2, gogrid), identity (ex. accesskey, username), credential (ex. secretkey, password), then the name of the group you'd like to add the node to, running minecraft.

java -jar target/minecraft-compute-jar-with-dependencies.jar provider identity credential mygroup add

java -jar target/minecraft-compute-jar-with-dependencies.jar provider identity credential mygroup add

java -jar target/minecraft-compute-jar-with-dependencies.jar provider identity credential mygroup destroy

Ex. for GleSYS

java -jar target/minecraft-compute-jar-with-dependencies.jar glesys user apikey mygroup add

Ex. for Amazon EC2

java -jar target/minecraft-compute-jar-with-dependencies.jar aws-ec2 accesskey secretkey mygroup add

## Playing

Open Minecraft, go to Multiplayer, Direct Connect, and enter the ip address of your cloud node.

## Notes

If you have a firewall blocking the remote ip:25565, you will need to port forward your local 25565 (probably over ssh)

Ex. if my cloud servers' ip is 15.185.168.16
ssh 15.185.168.16 -L 25565:15.185.168.16:22

## License

Copyright (C) 2009-2012 jclouds, Inc.

Licensed under the Apache License, Version 2.0

