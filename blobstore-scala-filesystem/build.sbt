name := "blobstore-scala-filesystem"

scalaVersion := "2.10.2"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq( "org.apache.jclouds.api" % "filesystem" % "1.6.1-incubating",
                             "com.google.code.findbugs" % "jsr305" % "1.3.+",
                             "com.jsuereth" %% "scala-arm" % "1.3"
                            )
