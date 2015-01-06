# blobstore-uploader

This is a simple example command line client that attempts to upload many small to medium sized files to a provider supported by jclouds. 

## Build

```
mvn clean install
```

## Run

This is an example command line:
```
java -jar .\target\blob-uploader-1.0-SNAPSHOT.jar --username bob --password 701addc5-14ff-474b-9ffc-23e8e91b46a4 --provider "rackspace-cloudfiles-us" --region DFW --directory "P:\somedirectory"
```

You can also specify the number of upload threads with --threads

## License

Copyright (C) 2009-2014 The Apache Software Foundation

Licensed under the Apache License, Version 2.0
