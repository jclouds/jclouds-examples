# Camel Notifier

This simple examples demonstrates using Apache Camel with jclouds.
It starts a camel route which polls the cloud provider for running nodes.
If the clouds provider gives a possitive response it sends an email to the user notifying him, that there are nodes running.

# Setup
The following properties need to be added the maven profile or to the project pom:

jclouds.aws.identity
jclouds.aws.credential
jclouds.aws.region

smtp.user
smtp.password
smtp.server

Finally, you will need to modify camel-context.xml to set the form/to email addresses.

# Running the sample

from the command line just type: mvn camel:run
