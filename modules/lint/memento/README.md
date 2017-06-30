# Memento

This project consists of an annotation processor for Android Lint's IssueRegistry generation and a Gradle plugin
to automatically register the IssueRegistry in the rules MANIFEST.MF file.

## Gradle Plugin

The plugin is published to our internal maven repository. To install a new version to that repository the following
command can be used:
```text
./gradlew :modules:lint:memento:plugin:publishReleasePublicationToMavenRepository
```
A new Gradle module can make use of that plugin by adding the following to their build file:
```groovy
buildscript {
    repositories {
        maven { url 'http://maven.int.s-cloud.net/content/groups/soundcloud-proxy' }
    }
    dependencies {
        classpath 'com.soundcloud:memento:0.1-SNAPSHOT'
    }
    apply plugin: 'com.soundcloud.memento'
}
```
The plugin expects to have the 'java' plugin applied to the project. If that is not the case, it won't have any effect.
Besides that the project needs to use the following annotation processor to generate the expected manifest configuration
file.

## Annotations

The `annotation` module consists of three annotations:
* `LintRegistry`<br>
This annotation is optional and just necessary if customizations to the `IssueRegistry` are required. In that case
an abstract class extending Android Lint's `IssueRegistry` can be annotated to generate the `getIssues()` method. All
other methods can be adjusted.

* `LintDetector`<br>
Each new Android Lint `Detector` needs to be annotated such that the contained constant (at least static and
package-private) `Issue`'s will be added. An `Issue` of a `Detector` can optionally be excluded by adding the 
`Exclude` annotation to it.

* `Exclude`<br>
In case that an `Issue` should not yet be picked up, it can be excluded by adding this annotation to it.

## Adding the annotation processor

Since new Lint `Detector`'s can currently just be created as part of a plain `java` module, both the annotations and the
compiler dependencies need to be added as compile dependencies to the project.

With `apt-plugin`:
```groovy
apply plugin: 'net.ltgt.apt'

dependencies {
    apt 'com.soundcloud:memento-compiler:1.0.0'
    compileOnly 'com.soundcloud:memento:1.0.0'
}
```

Without `apt-plugin`:
```groovy
dependencies {
    compile 'com.soundcloud:memento-compiler:1.0.0'
    compile 'com.soundcloud:memento:1.0.0'
}
```
