import sbt._
import scala.xml.{Node, Elem, Unparsed}

trait Mavenize extends DefaultProject {
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
          <executions>
            <execution>
              <phase>install</phase>
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
              <phase>install</phase>
              <goals>
                <goal>copy-dependencies</goal>
              </goals>
              <configuration>
                <outputDirectory>tests/lib</outputDirectory>
                <excludeTransitive>false</excludeTransitive>
                <includeScope>test</includeScope>
              </configuration>
            </execution>
          </executions>
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
                      <archiveDirectory></archiveDirectory>
                      <includes>
                        <include>target/*.apk</include>
                      </includes>
                      <keystore>soundcloud_sign/soundcloud.ks</keystore>
                      <alias>jons keystore</alias>
                    </configuration>
                  </execution>
                </executions>
              </plugin>
              <plugin>
                <groupId>com.jayway.maven.plugins.android.generation2</groupId>
                <artifactId>maven-android-plugin</artifactId>
                <inherited>true</inherited>
                <configuration>
                  <sign>
                    <debug>false</debug>
                  </sign>
                </configuration>
              </plugin>
            </plugins>
          </build>
        </profile>
      </profiles>

    override def makePomConfiguration = new MakePomConfiguration(deliverProjectDependencies,
                                            Some(List(Configurations.Compile,
                                                      Configurations.Provided,
                                                      Configurations.Test)),
                                                      pomExtra, pomPostProcess, pomIncludeRepository)

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
