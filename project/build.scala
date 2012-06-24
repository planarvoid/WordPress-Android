import sbt._
import Keys._
import AndroidKeys._
import AndroidNdkKeys._

object General {
  val settings = Defaults.defaultSettings ++ Seq(
    organization := "com.soundcloud.android",
    platformName := "android-14"
  )

  val androidProjectSettings =
    settings ++
    AndroidProject.androidSettings ++
    PlainJavaProject.settings ++
    AndroidMarketPublish.settings ++
    Github.settings ++
    PasswordManager.settings ++
    AndroidInstall.settings
}

object AndroidBuild extends Build {
  val junit_interface = "com.novocode" % "junit-interface" % "0.8" % "test" intransitive()
  val jacksonVersion = "2.0.1"
  val coreDependencies = Seq(
    "org.acra" % "acra" % "4.3.0-filter-SNAPSHOT",
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
    "com.soundcloud" % "java-api-wrapper" % "1.1.2-SNAPSHOT",
    "com.google.android" % "filecache" % "r153",
    "com.commonsware" % "CWAC-AdapterWrapper" % "0.4",
    "com.at" % "ATInternet" % "1.1.003",
    "com.google.android" % "support-v4" % "r6",
    "com.google.android" % "android" % "4.0.1.2" % "provided",
    "com.intellij" % "annotations" % "9.0.4" % "compile",
    "com.soundcloud.android" % "cropimage" % "1.1.0" artifacts(Artifact("cropimage", "apklib", "apklib"))
  )

  val testDependencies = Seq(
    "com.pivotallabs" % "robolectric" % "1.2-SNAPSHOT" % "test",
    "junit" % "junit-dep" % "4.9" % "test",
    "org.mockito" % "mockito-core" % "1.8.5" % "test",
    "com.github.xian" % "great-expectations" % "0.13" % "test",
    "org.scalatest" %% "scalatest" % "1.7.1" % "test",
    "org.scala-lang" % "scala-compiler" % "2.9.1" % "test",
    junit_interface
  )

  val integrationTestDependencies = Seq(
    "com.jayway.android.robotium" % "robotium-solo" % "3.3" % "int,compile",
    junit_interface
  )

  val repos = Seq(
    MavenRepository("sc int repo", "http://files.int.s-cloud.net/maven/"),
    MavenRepository("sonatype snapshots", "https://oss.sonatype.org/content/repositories/snapshots"),
    MavenRepository("sonatype releases", "https://oss.sonatype.org/content/repositories/releases"),
    MavenRepository("Local Maven Repository", "file://" + Path.userHome + "/.m2/repository")
  )

  val projectSettings = General.androidProjectSettings ++ AndroidNdk.settings ++ Seq(
      libraryDependencies ++= coreDependencies ++ testDependencies,
      resolvers          ++= repos,
      compileOrder       := CompileOrder.JavaThenScala,
      unmanagedBase      <<= baseDirectory / "lib-unmanaged", // make sure dl'ed libs don't get picked up
      javaSource in Compile         <<= (baseDirectory) (_ / "src" / "main" / "java")
    ) ++ inConfig(Android)(Seq(
      keyalias           := "jons keystore",
      keystorePath       <<= (baseDirectory) (_ / "soundcloud_sign" / "soundcloud.ks"),
      githubRepo         := "soundcloud/SoundCloud-Android",
      cachePasswords     := true,
      nativeLibrariesPath <<= (baseDirectory) (_ / "libs"),
      jniSourcePath <<= baseDirectory / "jni",
      jniClasses := Seq(
        "com.soundcloud.android.jni.VorbisEncoder",
        "com.soundcloud.android.jni.VorbisDecoder"
      ),
      javahOutputDirectory <<= (baseDirectory) (_ / "jni" / "include" / "soundcloud")
    )) ++ inConfig(Test)(Seq(
      javaSource         <<= (baseDirectory) (_ / "src" / "test" / "java"),
      scalaSource        <<= (baseDirectory) (_ / "src" / "test" / "scala"),
      resourceDirectory  <<= (baseDirectory) (_ / "src" / "test" / "resources"),
      parallelExecution  := false,
      unmanagedClasspath := Seq.empty
    ))

  // main project
  lazy val soundcloud_android = Project(
    "soundcloud-android",
    file("app"),
    settings = projectSettings ++ Mavenizer.settings ++ CopyLibs.settings ++ AmazonHelper.settings
  )

  // beta project
  lazy val soundcloud_android_beta = Project(
    "soundcloud-android-beta",
    file("app"),
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
      javaSource       in Compile <<= (baseDirectory) (_ / "src" / "main" / "java")
    ) ++ inConfig(Android)(Seq(
      useProguard    := false,
      // don't extract apk deps into test project. TODO: fix this in the plugin
      extractApkLibDependencies <<= (update in Compile, sourceManaged, managedJavaPath, resourceManaged, streams) map { (_, _, _, _, _) =>
        Seq.empty[LibraryProject]
      },
      proguardInJars := Seq.empty,
      mainResPath    <<= (baseDirectory, resDirectoryName) (_ / _) map (x=>x),
      mainAssetsPath <<= (baseDirectory, assetsDirectoryName) (_ / _),
      manifestPath   <<= (baseDirectory, manifestName) (_ / _) map (Seq(_)),
      dxInputs       <<= (compile in Compile, managedClasspath in Integration, classDirectory in Compile) map {
          (_, cp, classes) => cp.map(_.data) :+ classes
      }
    )) ++ CopyLibs.settings
  ).configs(Integration)
   .settings(inConfig(Integration)(Defaults.testSettings) : _*)
   .dependsOn(soundcloud_android)
}
