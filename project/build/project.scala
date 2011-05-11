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
    val acra_repo = "acra release repository" at
      "http://acra.googlecode.com/svn/repository/releases"

    val acra = "org.acra" % "acra" % "4.0.0b"
    val jackson_core = "org.codehaus.jackson" % "jackson-core-asl" % "1.7.1"
    val jackson_mapper = "org.codehaus.jackson" % "jackson-mapper-asl" % "1.7.1"
    val http_mime = "org.apache.httpcomponents" % "httpmime" % "4.1"
    val java_wrapper = "com.soundcloud" % "java-api-wrapper" % "1.0.0-SNAPSHOT"
  }
}
