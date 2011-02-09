# ec2-createlamp

This is a simple example command line client that creates a lamp server and everything you need to do that in ec2

## Build

Ensure you have maven 3.02 or higher installed, then execute 'mvn install' to build the sample.

## Run

Invoke the jar, passing your aws credentials and the name you wish to create or destroy

ex.
  java -jar target/ec2-createlamp-jar-with-dependencies.jar accesskey secretkey create adrianalmighty
  java -jar target/ec2-createlamp-jar-with-dependencies.jar accesskey secretkey destroy adrianalmighty

## License

Copyright (C) 2011 Cloud Conscious, LLC. <info@cloudconscious.com>

Licensed under the Apache License, Version 2.0 
