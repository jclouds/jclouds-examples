import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "play-compute"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "com.google.guava" % "guava" % "12.0-rc2",
      "org.jclouds" % "jclouds-compute" % "1.5.0-alpha.5"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      resolvers += "Maven central" at "http://repo1.maven.org/maven2/",
      resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    )

}
