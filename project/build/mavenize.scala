import sbt._
import scala.xml.{Node,Elem}

trait Mavenize extends ParentProject {
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
