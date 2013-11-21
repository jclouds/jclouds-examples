# jclouds-examples

This repository contains various examples of using
[jclouds](https://github.com/jclouds/jclouds).

<table>
  <thead><tr><th>project</th><th>description</th></tr></thead>
  <tbody>
      <tr>
      <td><a href="cloudwatch-basics/">Cloudwatch Basics (Java)</a></td>
      <td>Example code to get the total metrics stored for each of your instances within the past 24 hours and shows avg/max/min CPU utilization for each instance when possible.</td>
    </tr>
    <tr>
      <td><a href="compute-basics/">Compute Basics (Java)</a></td>
      <td>Example code to add nodes to a group and execute commands on them using the portable <a href="http://www.jclouds.org/documentation/userguide/compute/">ComputeService API</a></td>
    </tr>
    <tr>
      <td><a href="compute-clojure/">Compute Basics (Clojure)</a></td>
      <td>Example code using <a href="https://github.com/jclouds/jclouds/blob/master/compute/src/main/clojure/org/jclouds/compute2.clj">compute2</a> to create a node, execute a command and destroy the node.</td>
    </tr>
    <tr>
      <td><a href="minecraft-compute/">Start a Minecraft Server (Java)</a></td>
      <td>Example code to add nodes to a group and start Minecraft servers on them using the portable <a href="http://www.jclouds.org/documentation/userguide/compute/">ComputeService API</a></td>
    </tr>
    <tr>
      <td><a href="blobstore-basics/">BlobStore Basics (Java)</a></td>
      <td>Example code to create a container, blob, and list your blobs using the portable <a href="http://www.jclouds.org/documentation/userguide/blobstore-guide/">BlobStore API</a></td>
    </tr>
    <tr>
      <td><a href="blobstore-clojure/">BlobStore Basics (Clojure)</a></td>
      <td>Example code to create a container, and list your containers using the portable <a href="https://github.com/jclouds/jclouds/blob/master/blobstore/src/main/clojure/org/jclouds/blobstore2.clj">blobstore2 functions</a></td>
    </tr>
    <tr>
      <td><a href="blobstore-scala-filesystem/">BlobStore Basics (Scala)</a></td>
      <td>Example code to create a container and blob using the filesystem <a href="http://jclouds.apache.org/documentation/userguide/blobstore-guide/">BlobStore API</a></td>
    </tr>
    <tr>
      <td><a href="blobstore-karaf-shell">BlobStore via Karaf Shell</a></td>
      <td>Example to read and write to a blobstore from inside the Apache Karaf Shell.</td>
    </tr>
    <tr>
      <td><a href="ec2-computeservice-spot/">Use EC2 Extensions in ComputeService (Java)</a></td>
      <td>Example code to create a spot instance on <a href="http://www.jclouds.org/documentation/userguide/using-ec2/">EC2</a> using <a href="http://www.jclouds.org/documentation/userguide/compute/">ComputeService API</a> extensions</td>
    </tr>
    <tr>
      <td><a href="ec2-createlamp/">EC2 Create LAMP (Java)</a></td>
      <td>Example code to create a LAMP server on <a href="http://www.jclouds.org/documentation/userguide/using-ec2/">EC2</a> using the provider-specific EC2Client</td>
    </tr>
    <tr>
      <td><a href="deploy-war-via-ant/">Deploy a webapp (ant)</a></td>
      <td>Example code to deploy a web application to the cloud using <a href="http://cargo.codehaus.org/">Cargo</a> and the jclouds ant plugin</td>
    </tr>
    <tr>
      <td><a href="blobstore-largeblob/">Large Blob support (Java)</a></td>
      <td>Example code to create a container and use a parallel strategy to upload a large blob, the portable <a href="http://www.jclouds.org/documentation/userguide/blobstore-guide/">BlobStore API</a></td>
    </tr>
    <tr>
      <td><a href="camel-notifications/">Camel notifications</a></td>
      <td>Example code that uses jclouds from inside Apache Camel routes. The example provides routes that poll the compute provider for running nodes and sends notifications via email. </td>
    </tr>
    <tr>
      <td><a href="rackspace/">Rackspace (Java)</a></td>
      <td>Example code that uses jclouds to perform common tasks on the <a href="http://www.jclouds.org/documentation/quickstart/rackspace/">Rackspace Cloud</a>.</td>
    </tr>
    <tr>
      <td><a href="chef-basics/">Chef Basics (Java)</a></td>
      <td>Example code to add nodes to a group and execute Chef cookbooks on them using Chef Solo or a standard or Hosted Chef Server.</td>
    </tr>
  </tbody>
</table>

![jclouds logo](http://jclouds.apache.org/style/fullsizejcloudslogo.jpg)

## License

Copyright (C) 2011 Cloud Conscious, LLC. <info@cloudconscious.com>

Licensed under the Apache License, Version 2.0
