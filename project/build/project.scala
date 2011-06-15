import sbt._

trait Defaults {
  def androidPlatformName = "android-10"
}

class Parent(info: ProjectInfo) extends ParentProject(info) {
  override def shouldCheckOutputDirectories = false
  override def updateAction = task { None }

  lazy val main  = project(".", "soundcloud-android", new MainProject(_))

  class MainProject(info: ProjectInfo) extends AndroidProject(info)
    with Defaults
    with MarketPublish
    with PlainJavaProject
    with Mavenize {

    val keyalias  = "change-me"

    val sc_repo   = "sc int repo" at "http://files.int.s-cloud.net/maven/"
    val acra_repo = "acra release repository" at
        "http://acra.googlecode.com/svn/repository/releases"
    val oss_repo1  = "sonatype snapshots" at
        "https://oss.sonatype.org/content/repositories/snapshots"
    val oss_repo2  = "sonatype releases" at
        "https://oss.sonatype.org/content/repositories/releases"


    // core dependencies
    val acra = "org.acra" % "acra" % "3.1.2"
    val jackson_core = "org.codehaus.jackson" % "jackson-core-asl" % "1.7.1"
    val jackson_mapper = "org.codehaus.jackson" % "jackson-mapper-asl" % "1.7.1"
    val java_wrapper = "com.soundcloud" % "java-api-wrapper" % "1.0.1-SNAPSHOT"
    val filecache = "com.google.android" % "filecache" % "r153"
    val analytics = "com.google.android" % "libGoogleAnalytics" % "1.2"
    var wrapper   = "com.commonsware" % "CWAC-AdapterWrapper" % "0.4"
    val vorbis    = "org.xiph" % "libvorbis" % "1.0.0-beta"

    // test dependencies
    val robolectric = "com.pivotallabs" % "robolectric" % "1.0-RC2-SNAPSHOT-all" % "test"
    val junit       = "junit" % "junit-dep" % "4.9b2" % "test"
    val mockitoCore = "org.mockito" % "mockito-core" % "1.8.5" % "test"
    val hamcrest    = "org.hamcrest" % "hamcrest-core" % "1.1" % "test"


    // provided dependencies
    val android   = "com.google.android" % "android" % "2.3.3" % "provided"
    val httpcore   = "org.apache.httpcomponents" % "httpcore" % "4.0.1" % "provided"
    val httpclient = "org.apache.httpcomponents" % "httpclient" % "4.0.3" % "provided"
    val json       = "org.json" % "json" % "20090211" % "provided"
    val logging    = "commons-logging" % "commons-logging" % "1.1.1" % "provided"
    val codec      = "commons-codec" % "commons-codec" % "1.5" % "provided"
  }
}
