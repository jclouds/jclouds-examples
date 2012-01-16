# compute-clojure

Basic usage of org.jclouds.compute2 to create a node, execute commands and destroy the node. The example below uses Amazon EC2 as provider.

## Build

Ensure you have [Leiningen](http://github.com/technomancy/leiningen) installed, then execute 'lein deps' to grab the jclouds dependencies. 

## Run

    bash$ lein repl
    user> (use 'org.jclouds.compute2)
    user> (use 'compute-clojure.compute-examples)
    user> (def compute (compute-service "aws-ec2" "AMAZON-IDENTITY" "AMAZON-CREDENTIAL" :slf4j :sshj))
    user> (create compute "example-node-group")
    user> (exec compute "echo hello" "example-node-group" (get-credentials))
    user> (destroy compute "example-node-group")

## License

Copyright (C) 2011 Cloud Conscious, LLC. <info@cloudconscious.com>

Licensed under the Apache License, Version 2.0 
