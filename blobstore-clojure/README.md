# blobstore-clojure

Basic blobstore usage example including creating a container and then listing all containers in your account.

## Build

Ensure you have [Leiningen](http://github.com/technomancy/leiningen) installed, then execute 'lein deps' to grab the jclouds dependencies. 

## Run

    bash$ lein repl
    user> (use 'create-list.containers)
    user> (create-and-list "transient" "foo" "bar" "mycontainer")
       or for a real blobstore like S3
    user> (create-and-list "aws-s3" accesskey secret "mybucket")

## License

Copyright (C) 2009-2012 jclouds, Inc.

Licensed under the Apache License, Version 2.0 

