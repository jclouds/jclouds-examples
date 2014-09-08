# OpenStack Examples
Example code that uses jclouds to perform common tasks on an OpenStack Cloud. The class names are self-explanatory and the code is well commented for you to follow along.

- [Requirements](#requirements)
- [Environment](#environment)
- [The Examples](#examples)
- [Support and Feedback](#support-and-feedback)

## Requirements

1. Username and password for an OpenStack Cloud - See the [Getting Started guide](http://jclouds.apache.org/guides/openstack/).
1. Java Development Kit (JDK) version 6 or later - [Download](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
1. Apache Maven - [Maven in 5 Minutes](http://maven.apache.org/guides/getting-started/maven-in-five-minutes.html).
1. Git - [Download](http://git-scm.com/downloads).

## Environment
To setup an environment to compile and run the examples use these commands:

```
git clone https://github.com/jclouds/jclouds-examples.git
cd jclouds-examples/openstack/
```

To download all dependencies, run:

```
mvn dependency:copy-dependencies "-DoutputDirectory=./lib"
```

If you also want to download the source jars, run:

```
mvn dependency:copy-dependencies "-DoutputDirectory=./lib" "-Dclassifier=sources"
```

## Examples

To run individual examples from the command line use these commands:

Note: If you're on Windows, the only change you need to make is to use a ';' instead of a ':' in the paths.

```
javac -classpath "lib/*:src/main/java/:src/main/resources/" src/main/java/org/jclouds/examples/openstack/identity/*.java
```

Every example class has a main method that takes the following arguments in the listed order

1. Identity (Keystone) endpoint (e.g. an IP address or URL)
1. Tenant name
1. User name
1. Password

Try out an example.

```
java -classpath "lib/*:src/main/java/:src/main/resources/" org.jclouds.examples.openstack.identity.CreateTenantAndUser identityEndpoint myTenantname myUsername myPassword
```
Watch the terminal for output!

## Support and Feedback

Your feedback is appreciated! If you have specific issues with OpenStack support in jclouds, we'd prefer that you file an issue via [JIRA](https://issues.apache.org/jira/browse/JCLOUDS).

If you have questions or need help, please join our [community](http://jclouds.apache.org/community/) and subscribe to the jclouds user mailing list.
