resolvers ++= Seq(
  Resolver.file(System.getProperty("user.home") + "/.ivy2/local"),
  "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)

addSbtPlugin("org.scala-tools.sbt" % "sbt-android-plugin" % "0.6.1-SNAPSHOT")
