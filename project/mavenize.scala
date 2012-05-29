import sbt._
import Keys._
import AndroidKeys._
import scala.xml.{Node, Elem, Unparsed}

object Mavenizer {
  val pom =
    <parent>
      <groupId>com.soundcloud.android</groupId>
      <artifactId>parent</artifactId>
      <version>{versionName}</version>
    </parent> ++
    <build>
     <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>2.3</version>
        </plugin>

        <plugin>
          <artifactId>maven-dependency-plugin</artifactId>
          <version>2.4</version>
          <executions>
            <execution>
              <phase>process-resources</phase>
              <goals>
                <goal>copy-dependencies</goal>
              </goals>
              <configuration>
                <outputDirectory>lib</outputDirectory>
                <excludeTransitive>false</excludeTransitive>
                <includeScope>runtime</includeScope>
              </configuration>
            </execution>
          </executions>
        </plugin>
      <plugin>
        <artifactId>maven-clean-plugin</artifactId>
          <version>2.4.1</version>
          <configuration>
            <filesets>
              <fileset> <directory>lib</directory> </fileset>
            </filesets>
          </configuration>
      </plugin>
      </plugins>
    </build>

    def doPomPostProcess(pom: Node, name: String, version: String): Node = pom match {
      case <artifactId>{_}</artifactId> => <artifactId>{name}</artifactId>
      case <version>{_}</version>       => <version>{version}</version>
      case <packaging>{_}</packaging>   => <packaging>apk</packaging>
      case Elem(prefix, "project", attributes, scope,  c @ _*) =>
        Elem(prefix, "project", attributes, scope, c.map(doPomPostProcess(_, name, version)):_*)
      case other => other
    }

    val mavenize = TaskKey[File]("mavenize", "mavenizes the project")

    lazy val settings:Seq[Setting[_]] = Seq(
      pomExtra := pom,
      pomPostProcess <<= (version) ( (v) => (node =>
        doPomPostProcess(node, "soundcloud-android", versionName)
      )),
      makePomConfiguration <<= (artifactPath in makePom, projectInfo, pomExtra,
                                pomPostProcess, pomIncludeRepository,
                                pomAllRepositories) {
         (file, minfo, extra, process, include, all) =>
          new MakePomConfiguration(file, minfo, Some(List(Configurations.Compile,
                                                   Configurations.Provided)), extra, process, include, all)
      },
      mavenize <<= (makePom, baseDirectory, target) map { (pom, d, outputPath) =>
        val pomPath = d / "pom.xml"
        IO.copyFile(pom, pomPath)
        pomPath
      }
    )

    lazy val versionName = scala.xml.XML.loadFile("app/AndroidManifest.xml")
                .attribute("http://schemas.android.com/apk/res/android", "versionName")
                .map(_.text)
                .getOrElse(sys.error("no versionName"))
}
