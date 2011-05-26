# compute-clojure

Basic usage of org.jclouds.compute2 to create a node, execute commands and destroy the node. The example below uses Amazon EC2 as provider.

## Build

Ensure you have [Leiningen](http://github.com/technomancy/leiningen) installed, then execute 'lein deps' to grab the jclouds dependencies. 

## Run

    bash$ lein repl
    user> (use 'org.jclouds.compute2)
    user> (use 'compute-clojure)
    user> (def compute "aws-ec2" "AMAZON-IDENTITY" "AMAZON-CREDENTIAL" :ssh)
    user> (add compute "example-node-group")
	user> (exec compute "echo hello" "example-node-group" (get-credentials))
	user> (destroy compute "example-node-group")

## License

Copyright (C) 2011 Cloud Conscious, LLC. <info@cloudconscious.com>

Licensed under the Apache License, Version 2.0 