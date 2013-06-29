/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import org.jclouds.blobstore.BlobStoreContext
import org.jclouds.ContextBuilder
import org.jclouds.filesystem.reference.FilesystemConstants
import resource._

/**
 * Demonstrates the use of the filesystem [[org.jclouds.blobstore.BlobStore]] in Scala
 *
 * Usage is: run &lt;basedir&gt; &lt;containername&gt; &lt;blobname&gt;
 *
 * @author adisesha
 */
object Main extends App {
  require(args.length == 3, "Invalid number of parameters. Usage: run <basedir> <containername> <blobname>")

  val basedir = args(0)
  val containerName = args(1)
  val blobname = args(2)

  val properties = new java.util.Properties()
  properties.setProperty(FilesystemConstants.PROPERTY_BASEDIR, basedir)

  //Using scala-arm for context management. See https://github.com/jsuereth/scala-arm
  managed(ContextBuilder.newBuilder("filesystem")
    .overrides(properties)
    .buildView(classOf[BlobStoreContext]))
    .acquireAndGet(context => {

    val blobStore = context.getBlobStore
    blobStore.createContainerInLocation(null, containerName)

    val blob = blobStore.blobBuilder(blobname).payload("testdata").build()
    blobStore.putBlob(containerName, blob)

    val filePath = basedir + System.getProperty("file.separator") + containerName
    println(s"Blob '$blobname' stored under '$filePath'")
  })
}
