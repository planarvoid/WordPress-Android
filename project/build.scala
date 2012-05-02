import sbt._
import Keys._
import AndroidKeys._

object General {
  val settings = Defaults.defaultSettings ++ Seq(
    organization := "com.soundcloud",
    platformName := "android-14"
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
  val jacksonVersion = "2.0.1"
  val coreDependencies = Seq(
    "org.acra" % "acra" % "4.3.0-filter-SNAPSHOT",
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
    "com.soundcloud" % "java-api-wrapper" % "1.1.1-SNAPSHOT",
    "com.google.android" % "filecache" % "r153",
    "com.commonsware" % "CWAC-AdapterWrapper" % "0.4",
    "org.xiph" % "libvorbis" % "1.0.0-beta",
    "com.at" % "ATInternet" % "1.1.003",
    "com.google.android" % "android" % "4.0.1.2" % "provided"
  )

  val testDependencies = Seq(
    "com.pivotallabs" % "robolectric" % "1.2-SNAPSHOT" % "test",
    "junit" % "junit-dep" % "4.9" % "test",
    "org.mockito" % "mockito-core" % "1.8.5" % "test",
    "com.github.xian" % "great-expectations" % "0.10" % "test",
    "com.novocode" % "junit-interface" % "0.7" % "test" intransitive(),
    "org.scalatest" %% "scalatest" % "1.7.1" % "test",
    "org.scala-lang" % "scala-compiler" % "2.9.1" % "test"
  )

  val integrationTestDependencies = Seq(
    "com.jayway.android.robotium" % "robotium-solo" % "3.1" % "int"
  )

  val repos = Seq(
    MavenRepository("sc int repo", "http://files.int.s-cloud.net/maven/"),
    MavenRepository("acra release repository", "http://acra.googlecode.com/svn/repository/releases"),
    MavenRepository("sonatype snapshots", "https://oss.sonatype.org/content/repositories/snapshots"),
    MavenRepository("sonatype releases", "https://oss.sonatype.org/content/repositories/releases")
  )

  // the main project
  val prepareAmazon = TaskKey[File]("prepare-amazon")

  val projectSettings = General.androidProjectSettings ++ Seq(
      libraryDependencies ++= coreDependencies ++ testDependencies,
      resolvers          ++= repos,
      compileOrder       := CompileOrder.JavaThenScala,
      unmanagedBase      <<= baseDirectory / "lib-unmanaged" // make sure dl'ed libs don't get picked up
    ) ++ inConfig(Android)(Seq(
      keyalias           := "jons keystore",
      keystorePath       <<= (baseDirectory) (_ / "soundcloud_sign" / "soundcloud.ks"),
      githubRepo         := "soundcloud/SoundCloud-Android",
      cachePasswords     := true,
      prepareAmazon      <<= (packageAlignedPath, streams) map { (path, s) =>
        if (AmazonHelper.isAmazon(path)) {
          val amazon = AmazonHelper.copy(path)
          s.log.success("Ready for Amazon appstore:\n"+amazon)
          amazon
        } else sys.error(path.getName+" is not an Amazon processed APK!")
      } dependsOn (AndroidMarketPublish.signReleaseTask, AndroidMarketPublish.zipAlignTask)
    )) ++ inConfig(Test)(Seq(
      javaSource         <<= (baseDirectory) (_ / "tests" / "src" / "java"),
      scalaSource        <<= (baseDirectory) (_ / "tests" / "src" / "scala"),
      resourceDirectory  <<= (baseDirectory) (_ / "tests" / "src" / "resources"),
      parallelExecution  := false,
      unmanagedClasspath := Seq.empty
    )) ++
      AndroidInstall.settings ++
      Mavenizer.settings

  // main project
  lazy val soundcloud_android = Project(
    "soundcloud-android",
    file("."),
    settings = projectSettings)

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
    "soundcloud-android-tests",
    file("tests-integration"),
    settings = General.settings ++
               AndroidTest.settings ++ Seq(
      name:= "Integration tests",
      libraryDependencies ++= integrationTestDependencies,
      resolvers ++= repos,
      javaSource       in Compile <<= (baseDirectory) (_ / "src" / "java"),
      managedClasspath in Compile <<= managedClasspath in Integration
    ) ++ inConfig(Android)(Seq(
      useProguard    := false,
      proguardInJars := Seq.empty,
      mainResPath    <<= (baseDirectory, resDirectoryName) (_ / _) map (x=>x),
      manifestPath   <<= (baseDirectory, manifestName) (_ / _) map (Seq(_)),
      dxInputs       <<= (compile in Compile, managedClasspath in Integration, classDirectory in Compile) map {
          (_, managedClasspath, classDirectory) => managedClasspath.map(_.data) :+ classDirectory
      }
    ))
  ).configs(Integration)
   .settings(inConfig(Integration)(Defaults.testSettings) : _*)
   .dependsOn(soundcloud_android)
}

object AmazonHelper {
    import collection.JavaConverters._
    import java.util.zip.ZipFile

    def isAmazon(f: File) = new ZipFile(f).entries.asScala.exists(_.getName.contains("com.amazon.content.id"))
    def copy(f: File) = {
        val amazon = new File(f.getParent, f.getName.replace(".apk", "-amazon.apk"))
        IO.copyFile(f, amazon)
        amazon
    }
}
