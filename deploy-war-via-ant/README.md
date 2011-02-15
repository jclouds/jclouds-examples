# deploy-war-via-ant

This example uses the jclouds [ComputeService](http://code.google.com/p/jclouds/wiki/ComputeGuide) ant plugin to create a new virtual machine (node) and the [Cargo](http://cargo.codehaus.org/) plugin to deploy the web application.

## Setup

Ensure you have Ant 1.7.1 installed and in your path.  Ex.

   wget http://archive.apache.org/dist/ant/binaries/apache-ant-1.7.1-bin.zip

   jar -xf apache-ant-1.7.1-bin.zip

   chmod 755 apache-ant-1.7.1/bin/*

   export PATH=apache-ant-1.7.1/bin:$PATH


Ensure you have jsch 0.1.42 is in $ANT_HOME/lib. Ex.

   cd apache-ant-1.7.1/lib

   wget https://sourceforge.net/projects/jsch/files/jsch/jsch-0.1.42.jar

## Run

### Deploying to localhost
Invoke 'ant justplaincargo' and this should deploy the webapp to http://localhost:8080/sample

### Deploying to the cloud
Invoke 'ant' and supply parameters when asked, or as system properties.

= provider - cloud you want to deploy to (ex. aws-ec2, cloudservers-us)

= identity - your account on the cloud provider (ex. accesskey, username)

= credential - your password on that account (ex. secretkey, password)

= group - what to name the node that runs your webapp (ex. cargo-webapp)

Ex. for Bluelock

ant -Dprovider=bluelock-vcdirector -Didentity=my@domain.com -Dcredential=password -Dgroup=cargo-webapp

Ex. for Amazon EC2

ant -Dprovider=aws-ec2 -Didentity=accesskey -Dcredential=secretkey -Dgroup=cargo-webapp


Note that you should run 'ant destroy' to cleanup cloud nodes after you are finished. 

## License

Copyright (C) 2011 Cloud Conscious, LLC. <info@cloudconscious.com>

