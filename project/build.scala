import sbt._
import Keys._
import AndroidKeys._
import AndroidNdkKeys._

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
  val junit_interface = "com.novocode" % "junit-interface" % "0.8" % "test" intransitive()

  val coreDependencies = Seq(
    "org.acra" % "acra" % "4.3.0-filter-SNAPSHOT",
    "org.codehaus.jackson" % "jackson-core-asl" % jacksonVersion,
    "org.codehaus.jackson" % "jackson-mapper-asl" % jacksonVersion,
    "com.soundcloud" % "java-api-wrapper" % "1.1.1-SNAPSHOT",
    "com.google.android" % "filecache" % "r153",
    "com.commonsware" % "CWAC-AdapterWrapper" % "0.4",
    "com.at" % "ATInternet" % "1.1.003",
    "com.google.android" % "support-v4" % "r6"
  )

  val testDependencies = Seq(
    "com.pivotallabs" % "robolectric" % "1.2-SNAPSHOT" % "test",
    "junit" % "junit-dep" % "4.9" % "test",
    "org.mockito" % "mockito-core" % "1.8.5" % "test",
    "org.hamcrest" % "hamcrest-core" % "1.1" % "test",
    "com.github.xian" % "great-expectations" % "0.10" % "test",
    "org.scalatest" %% "scalatest" % "1.7.1" % "test",
    "org.scala-lang" % "scala-compiler" % "2.9.1" % "test",
    "com.google.android" % "android" % "2.3.3" % "test",
    junit_interface
  )

  val integrationTestDependencies = Seq(
    "com.jayway.android.robotium" % "robotium-solo" % "3.2" % "int",
    junit_interface
  )

  val repos = Seq(
    MavenRepository("sc int repo", "http://files.int.s-cloud.net/maven/"),
    MavenRepository("acra release repository", "http://acra.googlecode.com/svn/repository/releases"),
    MavenRepository("sonatype snapshots", "https://oss.sonatype.org/content/repositories/snapshots"),
    MavenRepository("sonatype releases", "https://oss.sonatype.org/content/repositories/releases")
  )

  val projectSettings = General.androidProjectSettings ++ Seq(
      libraryDependencies ++= coreDependencies ++ testDependencies,
      resolvers          ++= repos,
      unmanagedBase      <<= baseDirectory / "lib-unmanaged" // make sure dl'ed libs don't get picked up
    ) ++ inConfig(Android)(Seq(
      keyalias           := "jons keystore",
      keystorePath       <<= (baseDirectory) (_ / "soundcloud_sign" / "soundcloud.ks"),
      githubRepo         := "soundcloud/SoundCloud-Android",
      cachePasswords     := true,
      nativeLibrariesPath <<= (baseDirectory) (_ / "libs")
    )) ++ inConfig(Test)(Seq(
      javaSource         <<= (baseDirectory) (_ / "tests" / "src" / "java"),
      scalaSource        <<= (baseDirectory) (_ / "tests" / "src" / "scala"),
      resourceDirectory  <<= (baseDirectory) (_ / "tests" / "src" / "resources"),
      parallelExecution  := false,
      unmanagedClasspath := Seq.empty
    )) ++ AndroidInstall.settings ++ AndroidNdk.settings

  // main project
  lazy val soundcloud_android = Project(
    "soundcloud-android",
    file("."),
    settings = projectSettings ++ inConfig(Android)(Seq(
      jniSourcePath <<= baseDirectory / "jni",
      jniClasses := Seq(
        "com.soundcloud.android.jni.VorbisEncoder",
        "com.soundcloud.android.jni.VorbisDecoder"
      ),
      javahOutputDirectory <<= (baseDirectory) (_ / "jni" / "include" / "soundcloud")
    )) ++ Mavenizer.settings ++ CopyLibs.settings ++ AmazonHelper.settings
  )

  // beta project
  lazy val soundcloud_android_beta = Project(
    "soundcloud-android-beta",
    file("."),
    settings = projectSettings ++ Seq(
      keyalias in Android := "beta-key"
   ))

  // integration tests
  lazy val Integration = config("int")
  lazy val soundcloud_android_tests = Project(
    "tests-integration",
    file("tests-integration"),
    settings = General.settings ++
               AndroidTest.settings ++ Seq(
      name := "tests-integration",
      libraryDependencies ++= integrationTestDependencies,
      resolvers           ++= repos,
      unmanagedBase       <<= baseDirectory / "lib-unmanaged", // make sure dl'ed libs don't get picked up
      javaSource       in Compile <<= (baseDirectory) (_ / "src" / "java"),
      managedClasspath in Compile <<= managedClasspath in Integration,
      // also compile in Test for test discovery
      javaSource       in Test    <<= javaSource in Compile,
      managedClasspath in Test    <<= managedClasspath in Compile,
      compile          in Test    <<= (compile in Test) triggeredBy (compile in Compile)
    ) ++ inConfig(Android)(Seq(
      useProguard    := false,
      proguardInJars := Seq.empty,
      mainResPath    <<= (baseDirectory, resDirectoryName) (_ / _) map (x=>x),
      mainAssetsPath <<= (baseDirectory, assetsDirectoryName) (_ / _),
      manifestPath   <<= (baseDirectory, manifestName) (_ / _) map (Seq(_)),
      dxInputs       <<= (compile in Compile, managedClasspath in Integration, classDirectory in Compile) map {
          (_, managedClasspath, classDirectory) => managedClasspath.map(_.data) :+ classDirectory
      }
    )) ++ CopyLibs.settings
  ).configs(Integration)
   .settings(inConfig(Integration)(Defaults.testSettings) : _*)
   .dependsOn(soundcloud_android)
}
