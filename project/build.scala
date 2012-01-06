import sbt._
import Keys._
import AndroidKeys._

object General {
  val settings = Defaults.defaultSettings ++ Seq(
    version := "2.0.1-SNAPSHOT",
    organization := "com.soundcloud"
  )

  val androidSettings =
    settings ++
    Seq(
      platformName := "android-10"
    )

  val androidProjectSettings =
    androidSettings ++
    AndroidProject.androidSettings ++
    PlainJavaProject.settings ++
    AndroidMarketPublish.settings ++
    Github.settings ++
    PasswordManager.settings
}

object AndroidBuild extends Build {
  val jacksonVersion = "1.8.5"
  val coreDependencies = Seq(
    "org.acra" % "acra" % "4.3.0-filter-SNAPSHOT",
    "org.codehaus.jackson" % "jackson-core-asl" % jacksonVersion,
    "org.codehaus.jackson" % "jackson-mapper-asl" % jacksonVersion,
    "com.soundcloud" % "java-api-wrapper" % "1.1.1-SNAPSHOT",
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
    "com.pivotallabs" % "robolectric" % "1.1-SNAPSHOT" % "test" classifier "jar-with-dependencies",
    "junit" % "junit-dep" % "4.9" % "test",
    "org.mockito" % "mockito-core" % "1.8.5" % "test",
    "org.hamcrest" % "hamcrest-core" % "1.1" % "test",
    "com.github.xian" % "great-expectations" % "0.10" % "test",
    "com.novocode" % "junit-interface" % "0.7" % "test" intransitive()
  )

  val repos = Seq(
    MavenRepository("sc int repo", "http://files.int.s-cloud.net/maven/"),
    MavenRepository("acra release repository", "http://acra.googlecode.com/svn/repository/releases"),
    MavenRepository("sonatype snapshots", "https://oss.sonatype.org/content/repositories/snapshots"),
    MavenRepository("sonatype releases", "https://oss.sonatype.org/content/repositories/releases")
  )

  lazy val soundcloud_android = Project (
    "soundcloud-android",
    file("."),
    settings = General.androidProjectSettings ++ Seq (
      keyalias in Android := "jons keystore",
      keystorePath in Android <<= (baseDirectory) (_ / "soundcloud_sign" / "soundcloud.ks"),
      githubRepo in Android := "soundcloud/SoundCloud-Android",
      cachePasswords in Android := true,
      unmanagedBase <<= baseDirectory / "lib-unmanaged",
      libraryDependencies ++= coreDependencies ++ providedDependencies ++ testDependencies,
      resolvers ++= repos,
      compileOrder := CompileOrder.JavaThenScala,
      javaSource in Test <<= (baseDirectory) (_ / "tests" / "java" / "src"),
      resourceDirectory in Test <<= (javaSource in Test) (js => js),
      parallelExecution in Test := false,
      (excludeFilter in resources) in Test := "*.java", // does not work atm
      unmanagedClasspath in Test <<= (unmanagedClasspath in Test) map (cp => Seq.empty)
    ) ++ AndroidInstall.settings ++ Mavenizer.settings
  )
}
