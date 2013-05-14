# Rackspace Examples
Example code that uses jclouds to perform common tasks on the Rackspace Cloud. The class names are self explanatory and the code is well commented for you to follow along.

- [Requirements](#requirements)
- [Environment](#environment)
- [The Examples](#the-examples)
- [Command Line](#command-line)
- [Eclipse](#eclipse)
- [Next Steps](#next-steps)
- [Support and Feedback](#support-and-feedback)

## Requirements

1. Username and API key for the Rackspace Cloud - See the [Getting Started guide](http://www.jclouds.org/documentation/quickstart/rackspace/).
1. Java Development Kit (JDK) version 6 or later - [Download](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
1. Apache Maven - [Maven in 5 Minutes](http://maven.apache.org/guides/getting-started/maven-in-five-minutes.html).
1. Git - [Download](http://git-scm.com/downloads).

## Environment
To setup an environment to compile and run the examples use these commands:

```
git clone https://github.com/jclouds/jclouds-examples.git
cd jcloud-examples/rackspace/
$ pwd
/Users/username/jclouds-examples/rackspace

$ ls
pom.xml    README.md    images/    src/
```

## The Examples

Start with the [rackspace package](https://github.com/jclouds/jclouds-examples/tree/master/rackspace/src/main/java/org/jclouds/examples/rackspace). There you will find general purpose examples of things that are useful across all services.

  * [Logging.java](https://github.com/jclouds/jclouds-examples/blob/master/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java) - How to enable and configure logging.
  * [Authentication.java](https://github.com/jclouds/jclouds-examples/blob/master/rackspace/src/main/java/org/jclouds/examples/rackspace/Authentication.java) - How you can use your credentials to authenticate with the Rackspace Cloud.

The [cloudfiles package](https://github.com/jclouds/jclouds-examples/tree/master/rackspace/src/main/java/org/jclouds/examples/rackspace/cloudfiles) demonstrates how to accomplish common tasks for putting files in and getting files from the cloud.

  * [CloudFilesPublish.java](https://github.com/jclouds/jclouds-examples/blob/master/rackspace/src/main/java/org/jclouds/examples/rackspace/cloudfiles/CloudFilesPublish.java) - An end to end example of publishing a file on the internet with Cloud Files.
  * Other examples of creating, updating, listing, and deleting containers/objects.

The [cloudservers package](https://github.com/jclouds/jclouds-examples/tree/master/rackspace/src/main/java/org/jclouds/examples/rackspace/cloudservers) demonstrates how to accomplish common tasks for working with servers in the cloud.  

  * [CloudServersPublish.java](https://github.com/jclouds/jclouds-examples/blob/master/rackspace/src/main/java/org/jclouds/examples/rackspace/cloudservers/CloudServersPublish.java) - An end to end example of publishing a web page on the internet with Cloud Servers.
  * Other examples of creating, manipulating, listing, and deleting servers.

The [cloudblockstorage package](https://github.com/jclouds/jclouds-examples/tree/master/rackspace/src/main/java/org/jclouds/examples/rackspace/cloudblockstorage) demonstrates how to accomplish common tasks for working with block storage (aka volumes) in the cloud.  

  * [CreateVolumeAndAttach.java](https://github.com/jclouds/jclouds-examples/blob/master/rackspace/src/main/java/org/jclouds/examples/rackspace/cloudblockstorage/CreateVolumeAndAttach.java) - An end to end example of creating a volume, attaching it to a server, putting a filesystem on it, and mounting it for use to store persistent data.
  * Other examples of creating, manipulating, listing, and deleting volumes and snapshots.

The [cloudloadbalancers package](https://github.com/jclouds/jclouds-examples/tree/master/rackspace/src/main/java/org/jclouds/examples/rackspace/cloudloadbalancers) demonstrates how to accomplish common tasks for working with load balancers in the cloud.  

  * [CreateLoadBalancerWithExistingServers.java](https://github.com/jclouds/jclouds-examples/blob/master/rackspace/src/main/java/org/jclouds/examples/rackspace/cloudloadbalancers/CreateLoadBalancerWithExistingServers.java) - An end to end example of creating a load balancer and adding existing servers (nodes) to it.
  * [CreateLoadBalancerWithNewServers.java](https://github.com/jclouds/jclouds-examples/blob/master/rackspace/src/main/java/org/jclouds/examples/rackspace/cloudloadbalancers/CreateLoadBalancerWithNewServers.java) - An end to end example of creating a load balancer and adding new servers (nodes) to it.
  * Other examples of creating, manipulating, listing, and deleting load balancers and nodes.

The [clouddns package](https://github.com/jclouds/jclouds-examples/tree/master/rackspace/src/main/java/org/jclouds/examples/rackspace/clouddns) demonstrates how to accomplish common tasks for working with DNS in the cloud.  

  * [CreateDomains.java](https://github.com/jclouds/jclouds-examples/blob/master/rackspace/src/main/java/org/jclouds/examples/rackspace/clouddns/CreateDomains.java) - An example of creating domains with subdomains and records.
  * [CreateRecords.java](https://github.com/jclouds/jclouds-examples/blob/master/rackspace/src/main/java/org/jclouds/examples/rackspace/clouddns/CreateRecords.java) - An example of creating records and adding them to a domain.
  * [CRUDReverseDNSRecords.java](https://github.com/jclouds/jclouds-examples/blob/master/rackspace/src/main/java/org/jclouds/examples/rackspace/clouddns/CRUDReverseDNSRecords.java) - An example of creating a Cloud Server and a reverse DNS record (PTR) to go along with it.
  * Other examples of creating, manipulating, listing, and deleting domains and records.


## Command Line

To download all dependencies, run:

```
mvn dependency:copy-dependencies "-DoutputDirectory=./lib"
```

If you also want to download the source jars, run:

```
mvn dependency:copy-dependencies "-DoutputDirectory=./lib" "-Dclassifier=sources"
```

To run individual examples from the command line use these commands:

Note: If you're on Windows, the only change you need to make is to use a ';' instead of a ':' in the paths.

```
cd src/main/java/
javac -classpath ".:../../../lib/*:../resources/" org/jclouds/examples/rackspace/*.java
```

Every example class has a main method that takes your username as the first argument and your API key as the second argument. The one exception to this is the Authentication example that can take an optional third argument if you want to use your password for authentication.

Try out an example.

```
java -classpath ".:../../../lib/*:../resources/" org.jclouds.examples.rackspace.cloudservers.CreateServer myUsername myApiKey
```
Watch the terminal for output!

## Eclipse
To run these examples from Eclipse follow these instructions.

Create a new Java Project and choose jcloud-examples/rackspace/ as the Location.

![Eclipse: Create Java Project](https://raw.github.com/jclouds/jclouds-examples/master/rackspace/images/Eclipse1.png "Eclipse: Create Java Project")

This should create a project with the following Java Settings. Eclipse will have detected the lib directory and filled in all of the Libraries for you.

![Eclipse: Java Settings](https://raw.github.com/jclouds/jclouds-examples/master/rackspace/images/Eclipse2.png "Eclipse: Java Settings")

Try out an example.

1. Double click CreateContainer example file to open it.
1. Choose the Run > Run Configurations... menu item.
1. Press the plus icon to create a new launch configuration.
1. This will automatically create a launch configuration for CreateContainer.
1. Switch to the Arguments tab and enter your username and API key in the Program arguments field.

![Eclipse: Launch Config](https://raw.github.com/jclouds/jclouds-examples/master/rackspace/images/Eclipse3.png "Eclipse: Launch Config")

Click Run and watch the Console for the output!

## Next Steps

Some suggestions.

1. Change the examples to do different things that you want to do.
1. After running some examples, compare the output with what you see in the [Cloud Control Panel](https://mycloud.rackspace.com/).
1. Browse the [documentation](http://www.jclouds.org/documentation/) and have a look at the [Javadoc](http://demobox.github.com/jclouds-maven-site/latest/apidocs).
1. Return to the [Installation Guide](http://www.jclouds.org/documentation/userguide/installation-guide/) and have a look at the different ways to integrate jclouds with your project.
1. Join the [jclouds mailing list](https://groups.google.com/forum/?fromgroups#!forum/jclouds) or maybe even the [jclouds developer mailing list](https://groups.google.com/forum/?fromgroups#!forum/jclouds-dev).

Welcome to the jclouds [community](http://www.jclouds.org/documentation/community/)!

## Support and Feedback

Your feedback is appreciated! If you have specific issues with Rackspace support in jclouds, we'd prefer that you file an issue via [JIRA](https://issues.apache.org/jira/browse/JCLOUDS).

For general feedback and support requests, send an email to:

[sdk-support@rackspace.com](mailto:sdk-support@rackspace.com)
