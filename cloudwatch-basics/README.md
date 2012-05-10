# cloudwatch-basics

This is a simple example command line client to get the total metrics stored for each of your instances the past 24 hours and shows avg/max/min CPU utilization for each instance when possible.

## Build

Ensure you have maven 3.02 or higher installed, then execute 'mvn install' to build the example.

## Run

Invoke the jar, passing your aws credentials.  Here is an example:

java -jar target/cloudwatch-basics-jar-with-dependencies.jar accessKeyId secretKey

## License

Copyright (C) 2009-2012 jclouds, Inc.

Licensed under the Apache License, Version 2.0 
