# google-lb

This is a simple example command line client that demonstrates [Network Load Balancing](https://cloud.google.com/compute/docs/load-balancing/network/). It also demonstrates how to use JSON credentials to authenticate allocating resources on a Google project.

## Build

Ensure you have **maven 3.02** or higher installed, then execute `mvn install` to build the example.

## Run

Invoke the jar, passing in the path to a JSON key file and the **action** you wish to run.

Supported actions are:

- create
- request
- destroy
- delete_startup_script


To create the network loadbalancer, run:

    java -jar target/google-lb-jar-with-dependencies.jar ~/path/to/key.json create

To demostrate the network loadbalancer is working, run the following command with your forwarding rule IP address:

    while true; do curl -m1 $IP; done

Note: It may take some time for the setup to finish propigating.

If you dont know your forwarding rule IP address, run:
    
    java -jar target/google-lb-jar-with-dependencies.jar ~/path/to/key.json request

To destroy the allocated resources run:

    java -jar target/google-lb-jar-with-dependencies.jar ~/path/to/key.json destroy

## Getting a JSON key

To get a JSON key file you must first set up a project and create a service account. Direction can be found [here](https://developers.google.com/console/help/new/#serviceaccounts).

## License

Copyright (C) 2009-2014 The Apache Software Foundation

Licensed under the Apache License, Version 2.0
