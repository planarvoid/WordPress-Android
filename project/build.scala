import sbt._
import Keys._
import AndroidKeys._

object General {
  val settings = Defaults.defaultSettings ++ Seq(
    organization := "com.soundcloud",
    platformName := "android-10"
  )

  val androidProjectSettings =
    settings ++
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
    "com.commonsware" % "CWAC-AdapterWrapper" % "0.4",
    "org.xiph" % "libvorbis" % "1.0.0-beta",
    "com.at" % "ATInternet" % "1.0",
    "com.google.android" % "android" % "2.3.3" % "provided"
  )

  val testDependencies = Seq(
    "com.pivotallabs" % "robolectric" % "1.1-SNAPSHOT" % "test",
    "junit" % "junit-dep" % "4.9" % "test",
    "org.mockito" % "mockito-core" % "1.8.5" % "test",
    "org.hamcrest" % "hamcrest-core" % "1.1" % "test",
    "com.github.xian" % "great-expectations" % "0.10" % "test",
    "com.novocode" % "junit-interface" % "0.7" % "test" intransitive(),
    "org.scalatest" %% "scalatest" % "1.7.1" % "test",
    "org.scala-lang" % "scala-compiler" % "2.9.1" % "test"
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
      libraryDependencies ++= coreDependencies ++ testDependencies,
      resolvers ++= repos,
      compileOrder := CompileOrder.JavaThenScala,
      javaSource in Test <<= (baseDirectory) (_ / "tests" / "src" / "java"),
      scalaSource in Test <<= (baseDirectory) (_ / "tests" / "src" / "scala"),
      resourceDirectory in Test <<= (baseDirectory) (_ / "tests" / "src" / "resources"),
      parallelExecution in Test := false,
      unmanagedClasspath in Test <<= (unmanagedClasspath in Test) map (cp => Seq.empty)
    ) ++ AndroidInstall.settings
      ++ Mavenizer.settings
  )
}
