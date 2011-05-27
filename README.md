# jclouds-examples

This repository contains various examples of using
[jclouds](https://github.com/jclouds/jclouds).

<table>
  <thead><tr><th>project</th><th>description</th></tr></thead>
  <tbody>
    <tr>
      <td><a href="jclouds-examples/tree/master/compute-basics/">Compute Basics (Java)</a></td>
      <td>Example code to add nodes to a group and execute commands on them using the portable <a href="http://code.google.com/p/jclouds/wiki/ComputeGuide">ComputeService API</a></td>
    </tr>
    <tr>
      <td><a href="jclouds-examples/tree/master/compute-clojure/">Compute Basics (Clojure)</a></td>
      <td>Example code using <a href="https://github.com/jclouds/jclouds/blob/master/compute/src/main/clojure/org/jclouds/compute2.clj">compute2</a> to create a node, execute a command and destroy the node.</td>
    </tr>
    <tr>
      <td><a href="jclouds-examples/tree/master/blobstore-basics/">BlobStore Basics (Java)</a></td>
      <td>Example code to create a container, blob, and list your blobs using the portable <a href="http://code.google.com/p/jclouds/wiki/BlobStore">BlobStore API</a></td>
    </tr>
    <tr>
      <td><a href="jclouds-examples/tree/master/blobstore-clojure/">BlobStore Basic (Clojure)</a></td>
      <td>Example code to create a container, and list your containers using the portable <a href="https://github.com/jclouds/jclouds/blob/master/blobstore/src/main/clojure/org/jclouds/blobstore2.clj">blobstore2 functions</a></td>
    </tr>
    <tr>
      <td><a href="jclouds-examples/tree/master/blobstore-karaf-shell/">BlobStore via Karaf Shell</a></td>
      <td>Example to read and write to a blobstore from inside the Apache Karaf Shell.</td>
    </tr>
    <tr>
      <td><a href="jclouds-examples/tree/master/ec2-computeservice-spot/">Use EC2 Extensions in ComputeService (Java)</a></td>
      <td>Example code to create a spot instance on <a href="http://code.google.com/p/jclouds/wiki/EC2">EC2</a> using <a href="http://code.google.com/p/jclouds/wiki/ComputeGuide">ComputeService API</a> extensions</td>
    </tr>
    <tr>
      <td><a href="jclouds-examples/tree/master/ec2-createlamp/">EC2 Create LAMP (Java)</a></td>
      <td>Example code to create a LAMP server on <a href="http://code.google.com/p/jclouds/wiki/EC2">EC2</a> using the provider-specific EC2Client</td>
    </tr>
    <tr>
      <td><a href="jclouds-examples/tree/master/deploy-war-via-ant/">Deploy a webapp (ant)</a></td>
      <td>Example code to deploy a web application to the cloud using <a href="http://cargo.codehaus.org/">Cargo</a> and the jclouds ant plugin</td>
    </tr>
    <tr>
      <td><a href="jclouds-examples/tree/master/blobstore-largeblob/">Large Blob support (Java)</a></td>
      <td>Example code to create a container and use a parallel strategy to upload a large blob, the portable <a href="http://code.google.com/p/jclouds/wiki/BlobStore">BlobStore API</a></td>
    </tr>
  </tbody>
</table>

[jclouds logo](http://cloud.github.com/downloads/jclouds/jclouds/jclouds_centered.jpg)

## License

Copyright (C) 2011 Cloud Conscious, LLC. <info@cloudconscious.com>

Licensed under the Apache License, Version 2.0
