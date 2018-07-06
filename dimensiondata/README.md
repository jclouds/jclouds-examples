# Dimension Data Examples
Example code that uses jclouds to perform common tasks on an Dimension Data CloudControl. The class names are self-explanatory and the code is well commented for you to follow along.

- [Requirements](#requirements)
- [Environment](#environment)
- [The Examples](#examples)
- [Support and Feedback](#support-and-feedback)

## Requirements

1. Username and password for Dimension Data CloudControl - See the [Getting Started guide](http://jclouds.apache.org/guides/dimensiondata/).
1. Java Development Kit (JDK) version 6 or later - [Download](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
1. Apache Maven - [Maven in 5 Minutes](http://maven.apache.org/guides/getting-started/maven-in-five-minutes.html).
1. Git - [Download](http://git-scm.com/downloads).

## Environment
To setup an environment to compile and run the examples use these commands:

```
git clone https://github.com/jclouds/jclouds-examples.git
cd jclouds-examples/dimensiondata/
```

To package the examples jar file and dependencies run:

```
mvn package
```

## Examples

To run individual examples from the command line use these commands:

Every example class has a main method that takes the following arguments in the listed order:

1. API Endpoint
1. Username
1. Password

If there are other arguments required they will follow. The command line format looks like this:
```
java -cp target\dimensiondata-cloudcontrol-examples-<VERSION>-jar-with-dependencies.jar <MAIN_CLASS> apiEndpoint username password <PARAMETERS>
```

Try out an example.

```
java -cp target\dimensiondata-cloudcontrol-examples-<VERSION>-jar-with-dependencies.jar org.jclouds.examples.dimensiondata.cloudcontrol.DeployNetworkDomainVlanAndServer apiEndpoint username password
```

Watch the terminal for output!

## Support and Feedback

Your feedback is appreciated! If you have specific issues with Dimension Data CloudControl support in jclouds, we'd prefer that you file an issue via [JIRA](https://issues.apache.org/jira/browse/JCLOUDS).

If you have questions or need help, please join our [community](http://jclouds.apache.org/community/) and subscribe to the jclouds user mailing list.
