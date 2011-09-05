import sbt._
import Keys._
import AndroidKeys._

object General {
  val settings = Defaults.defaultSettings ++ Seq (
    version := "1.4.3-SNAPSHOT",
    organization := "com.soundcloud"
  )

  val androidSettings =
    settings ++
    Seq (
      platformName := "android-10"
    )

  val androidProjectSettings =
    androidSettings ++
    AndroidProject.androidSettings ++
    PlainJavaProject.settings
}

object AndroidBuild extends Build {
  val jacksonVersion = "1.8.5"
  val coreDependencies = Seq(
    "org.acra" % "acra" % "4.3.0-SNAPSHOT",
    "org.codehaus.jackson" % "jackson-core-asl" % jacksonVersion,
    "org.codehaus.jackson" % "jackson-mapper-asl" % jacksonVersion,
    "com.soundcloud" % "java-api-wrapper" % "1.0.2-SNAPSHOT",
    "com.google.android" % "filecache" % "r153",
    "com.google.android" % "libGoogleAnalytics" % "1.3",
    "com.commonsware" % "CWAC-AdapterWrapper" % "0.4",
    "org.xiph" % "libvorbis" % "1.0.0-beta"
  )

  val providedDependencies = Seq(
    "com.google.android" % "android" % "2.3.3" % "provided",
    "org.apache.httpcomponents" % "httpcore" % "4.0.1" % "provided",
    "org.apache.httpcomponents" % "httpclient" % "4.0.3" % "provided",
    "org.json" % "json" % "20090211" % "provided",
    "commons-logging" % "commons-logging" % "1.1.1" % "provided",
    "commons-codec" % "commons-codec" % "1.5" % "provided"
  )

  val testDependencies = Seq(
    "com.pivotallabs" % "robolectric" % "1.0-RC5-SNAPSHOT" % "test" classifier "jar-with-dependencies",
    "junit" % "junit-dep" % "4.9b2" % "test",
    "org.mockito" % "mockito-core" % "1.8.5" % "test",
    "org.hamcrest" % "hamcrest-core" % "1.1" % "test"
  )

  val repos = Seq(
    MavenRepository("sc int repo", "http://files.int.s-cloud.net/maven/"),
    MavenRepository("acra release repository", "http://acra.googlecode.com/svn/repository/releases"),
    MavenRepository("sonatype snapshots", "https://oss.sonatype.org/content/repositories/snapshots")
  )

  lazy val soundcloud_android = Project (
    "soundcloud-android",
    file("."),
    settings = General.androidProjectSettings ++ Seq (
      keyalias in Android := "change-me",
      libraryDependencies ++= coreDependencies ++ providedDependencies ++ testDependencies,
      resolvers ++= repos,
      compileOrder := CompileOrder.JavaThenScala
    ) ++ AndroidInstall.settings ++ Mavenizer.settings
  )
}
