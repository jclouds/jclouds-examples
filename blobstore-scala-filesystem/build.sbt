name :="blobstore-scala-filesystem"

scalaVersion :="2.10.1"

version :="1.0-SNAPSHOT"

libraryDependencies ++= Seq( "org.jclouds.api" % "filesystem" % "1.6.0",
                             "com.google.code.findbugs" % "jsr305" % "1.3.+",
                             "com.jsuereth" %% "scala-arm" % "1.3"
                            )
