import sbt._
import Keys._
import AndroidKeys._
import scala.xml.{Node, Elem, Unparsed}

object Mavenizer {
  val pom =
    <build>
      <sourceDirectory>src</sourceDirectory>
      <testSourceDirectory>tests/src/java</testSourceDirectory>
      <testResources>
        <testResource>
          <directory>tests/src/resources</directory>
        </testResource>
      </testResources>
      <plugins>

        <plugin>
          <groupId>com.jayway.maven.plugins.android.generation2</groupId>
          <artifactId>android-maven-plugin</artifactId>
          <version>3.0.2</version>
          <configuration>
            <sdk>
              <platform>10</platform>
            </sdk>
            <undeployBeforeDeploy>true</undeployBeforeDeploy>
            <deleteConflictingFiles>false</deleteConflictingFiles>
            <zipalign>
              <outputApk>{Unparsed("${project.build.directory}/${project.artifactId}-${project.version}-market.apk")}</outputApk>
            </zipalign>
          </configuration>
          <extensions>true</extensions>
          <executions>
            <execution>
              <id>alignApk</id>
              <phase>install</phase>
              <goals>
                <goal>zipalign</goal>
              </goals>
            </execution>
          </executions>
        </plugin>

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
            <execution>
              <id>test-libs</id>
              <phase>process-resources</phase>
              <goals>
                <goal>copy-dependencies</goal>
              </goals>
              <configuration>
                <outputDirectory>tests/lib</outputDirectory>
                <excludeTransitive>false</excludeTransitive>
                <includeScope>test</includeScope>
                <excludeArtifactIds>soundcloud-android</excludeArtifactIds>
                <overWriteSnapshots>true</overWriteSnapshots>
              </configuration>
            </execution>
          </executions>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>2.9</version>
          <configuration>
            <includes>
              <include>**/*Test.java</include>
            </includes>
          </configuration>
      </plugin>
      <plugin>
        <artifactId>maven-clean-plugin</artifactId>
          <version>2.4.1</version>
          <configuration>
            <filesets>
              <fileset> <directory>lib</directory> </fileset>
              <fileset> <directory>tests/lib</directory> </fileset>
            </filesets>
          </configuration>
      </plugin>
      </plugins>
    </build> ++
    <profiles>
        <profile>
          <id>sign</id>
          <build>
            <plugins>
              <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jarsigner-plugin</artifactId>
                <version>1.2</version>
                <executions>
                  <execution>
                    <id>signing</id>
                    <goals>
                      <goal>sign</goal>
                    </goals>
                    <phase>package</phase>
                    <inherited>true</inherited>
                    <configuration>
                      <removeExistingSignatures>true</removeExistingSignatures>
                      <archiveDirectory></archiveDirectory>
                      <includes>
                        <include>{Unparsed("${project.build.directory}/${project.artifactId}.apk")}</include>
                      </includes>
                      <keystore>soundcloud_sign/soundcloud.ks</keystore>
                      <alias>jons keystore</alias>
                    </configuration>
                  </execution>
                </executions>
              </plugin>
            </plugins>
          </build>
        </profile>
      </profiles>

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
                                                   Configurations.Provided,
                                                   Configurations.Test)), extra, process, include, all)
      },
      mavenize <<= (makePom, baseDirectory, target) map { (pom, d, outputPath) =>
        val pomPath = d / "pom.xml"
        IO.copyFile(pom, pomPath)
        pomPath
      }
    )

    lazy val versionName = scala.xml.XML.loadFile("AndroidManifest.xml")
                .attribute("http://schemas.android.com/apk/res/android", "versionName")
                .map(_.text)
                .getOrElse(sys.error("no versionName"))
}
