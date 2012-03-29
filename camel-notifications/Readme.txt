# Camel Notifier

This simple examples demonstrates using Apache Camel with jclouds.
It starts a camel route which polls the cloud provider for running nodes.
If the clouds provider gives a possitive response it sends an email to the user notifying him, that there are nodes running.

# Setup
The following properties need to be added the maven profile or to the project pom:

jclouds.provider (ex. aws-ec2, hpcloud-compute, bluelock-vcloud-zone01)
jclouds.identity (ex. accesskey, tenant:accesskey, user@org)
jclouds.credential (ex. secretkey, password)

smtp.username
smtp.password
smtp.server

email.from (your email address)
email.to (who this is being sent to)

# Running the sample

from the command line just type: mvn camel:run
