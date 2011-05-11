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
    with PlainJavaProject {

    val keyalias  = "change-me"

    val sc_repo   = "sc int repo" at "http://files.int.s-cloud.net/maven/"
    val acra_repo = "acra release repository" at "http://acra.googlecode.com/svn/repository/releases"
    val oss       = "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

    val acra = "org.acra" % "acra" % "3.1.2"
    val jackson_core = "org.codehaus.jackson" % "jackson-core-asl" % "1.7.1"
    val jackson_mapper = "org.codehaus.jackson" % "jackson-mapper-asl" % "1.7.1"
    val http_mime = "org.apache.httpcomponents" % "httpmime" % "4.1"
    val java_wrapper = "com.soundcloud" % "java-api-wrapper" % "1.0.0-SNAPSHOT"
    val filecache = "com.google" % "filecache" % "0.1"
    val analytics = "com.google" % "libGoogleAnalytics" % "1.1"
    var wrapper   = "com.commonsware" % "CWAC-AdapterWrapper" % "0.4"
    val vorbis    = "org.xiph" % "libvorbis" % "1.0.0-beta"
  }
}
