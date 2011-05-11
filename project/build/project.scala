import sbt._
import scala.xml.{Node,Elem}

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
    val oss_repo  = "sonatype snapshots" at
        "https://oss.sonatype.org/content/repositories/snapshots"

    val android   = "com.google.android" % "android" % "2.3.3" % "provided"
    val acra = "org.acra" % "acra" % "3.1.2"
    val jackson_core = "org.codehaus.jackson" % "jackson-core-asl" % "1.7.1"
    val jackson_mapper = "org.codehaus.jackson" % "jackson-mapper-asl" % "1.7.1"
    val http_mime = "org.apache.httpcomponents" % "httpmime" % "4.1"
    val java_wrapper = "com.soundcloud" % "java-api-wrapper" % "1.0.0-SNAPSHOT"
    val filecache = "com.google.android" % "filecache" % "0.1"
    val analytics = "com.google.android" % "libGoogleAnalytics" % "1.1"
    var wrapper   = "com.commonsware" % "CWAC-AdapterWrapper" % "0.4"
    val vorbis    = "org.xiph" % "libvorbis" % "1.0.0-beta"

    val robolectric = "com.pivotallabs" % "robolectric" % "1.0-RC2-SNAPSHOT-all" % "test"
    val junit       = "junit" % "junit-dep" % "4.9b2" % "test"
    val mockitoCore = "org.mockito" % "mockito-core" % "1.8.5" % "test"

    override def makePomConfiguration = new MakePomConfiguration(deliverProjectDependencies,
                                            Some(List(Configurations.Compile,
                                                      Configurations.Provided,
                                                      Configurations.Test)),
                                                      pomExtra, pomPostProcess, pomIncludeRepository)
    override def pomExtra =
      <build>
        <sourceDirectory>src</sourceDirectory>
        <testSourceDirectory>tests/src</testSourceDirectory>
        <testResources>
          <testResource>
            <directory>tests/src</directory>
          </testResource>
        </testResources>
        <plugins>
            <plugin>
                <groupId>com.jayway.maven.plugins.android.generation2</groupId>
                <artifactId>maven-android-plugin</artifactId>
                <version>2.8.4</version>
                <configuration>
                    <sdk>
                        <platform>10</platform>
                    </sdk>
                    <undeployBeforeDeploy>true</undeployBeforeDeploy>
                    <deleteConflictingFiles>true</deleteConflictingFiles>
                </configuration>
                <extensions>true</extensions>
          </plugin>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>2.3</version>
          </plugin>
          <plugin>
            <artifactId>maven-dependency-plugin</artifactId>
            <executions>
              <execution>
                <phase>install</phase>
                <goals>
                    <goal>copy-dependencies</goal>
                 </goals>
                 <configuration>
                    <outputDirectory>lib</outputDirectory>
                    <excludeTransitive>true</excludeTransitive>
                    <includeScope>runtime</includeScope>
                 </configuration>
                </execution>
              </executions>
          </plugin>
        </plugins>
      </build>

    override def pomPostProcess(pom: Node): Node = pom match {
      case <artifactId>{_}</artifactId> => <artifactId>{name}</artifactId>
      case <packaging>{_}</packaging>   => <packaging>apk</packaging>
      case Elem(prefix, "project", attributes, scope,  c @ _*) =>
        Elem(prefix, "project", attributes, scope, c.map(pomPostProcess(_)):_*)
      case other => other
    }

    lazy val mavenize = task {
      val pomPath = info.projectPath / "pom.xml"
      FileUtilities.touch(pomPath, log)
      (outputPath ** "*.pom").get.foreach(FileUtilities.copyFile(_, pomPath, log))
      None
    } dependsOn(makePom)
  }
}
