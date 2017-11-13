package com.soundcloud.android.memento

import nebula.test.IntegrationSpec
import org.apache.commons.io.FileUtils
import spock.lang.Ignore

class LibraryIntegrationSpec extends IntegrationSpec {

  private def rootPath;

  void setup() {
    rootPath = new File(".").absolutePath.replace("/modules/lint/integTest/.", "")
    buildFile << androidBuild("com.example.android")
    writeAndroidManifestWithActivity("com.example.android", "MainActivity")
    copyBuildsystemDir()

    addSubproject(":modules:android-kit:platform", "")
    addSubproject(":modules:android-kit:core", "")
    addSubproject(":modules:groupie:groupie-compiler", "")
    addSubproject(":modules:groupie:groupie", "")
    addSubproject(":modules:mrlocallocal", "")
    addSubproject(":modules:lint:memento:memento", "")
    addSubproject(":modules:lint:memento:compiler", "")
    addSubproject(":modules:lint:rules", "")
    addSubproject(":modules:lint:lib", "")
    addSubproject(":integTest", "")
    settingsFile << settingsGradle()
  }

  @Ignore // Gradle and Nebula don't play well together anymore. AGP 3.0 also causes issues in other PRs
  def 'lint rules picked up'() {
    when:
    def result = runTasksSuccessfully('lintDebug')

    then:
    fileExists('build/reports/lint-results-debug.xml')
    result.standardOutput.contains('src/main/java/com/example/android/MainActivity.java:12: Warning: Direct navigation should be replaced with Navigator call. [sc.StartIntent]')
    result.standardOutput.contains('src/main/java/com/example/android/MainActivity.java:12: Warning: Intent\'s should be created in the (Pending)IntentFactory. [sc.CreateIntent]')
  }

  protected String settingsGradle() {
    return """
              project(':modules:lint:lib').projectDir = file('$rootPath/modules/lint/lib')
              project(':modules:lint:rules').projectDir = file('$rootPath/modules/lint/rules')
              project(':modules:lint:memento:memento').projectDir = file('$rootPath/modules/lint/memento/memento')
              project(':modules:lint:memento:compiler').projectDir = file('$rootPath/modules/lint/memento/compiler')
           """.stripIndent()
  }

  protected String androidBuild(String applicationId) {
    return """\
            apply from: '$rootPath/buildsystem/dependencies.gradle'
            apply plugin: 'com.android.application'

            buildscript {
                apply from: '$rootPath/buildsystem/dependencies.gradle'
                def gradlePlugins = rootProject.ext.gradlePlugins
                
                repositories {
                    mavenLocal()
                    maven { url 'http://maven.int.s-cloud.net/content/groups/soundcloud-proxy' }
                }
                dependencies {
                    classpath gradlePlugins.android
                }
            }
            
            ext {
              javaSrcDirs = getProjectDir()
              resSrcDirs = getProjectDir()
            }
            
            repositories {
                mavenLocal()
                maven { url 'http://maven.int.s-cloud.net/content/groups/soundcloud-proxy' }
            }
            
            android {
                compileSdkVersion rootProject.ext.androidCompileSdkVersion
                buildToolsVersion rootProject.ext.androidBuildToolsVersion

                defaultConfig {
                    applicationId "${applicationId}"
                    targetSdkVersion rootProject.ext.androidTargetSdkVersion
                    minSdkVersion rootProject.ext.androidMinSdkVersion
                    versionCode 1
                    versionName "1.0"
            
                    testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
                }
                buildTypes {
                    release {
                        minifyEnabled false
                        proguardFiles getDefaultProguardFile('proguard-android.txt')
                    }
                }
                dependencies {
                    compile project(":modules:lint:lib")
                }
                lintOptions {
                    check 'sc.StartIntent', 'sc.CreateIntent'
                    abortOnError false
                    textReport true
                    textOutput 'stdout'
                }
            }
            
            allprojects {
                repositories {
                    mavenLocal()
                    maven { url 'http://maven.int.s-cloud.net/content/groups/soundcloud-proxy' }
                }
                buildscript {
                    repositories {
                        mavenLocal()
                        maven { url 'http://maven.int.s-cloud.net/content/groups/soundcloud-proxy' }
                    }
                }
            }
            """.stripIndent()
  }

  protected void writeAndroidManifestWithActivity(String packageDotted, String mainActivity, File baseDir = getProjectDir()) {
    def path = 'src/main/AndroidManifest.xml'
    def javaFile = createFile(path, baseDir)
    javaFile << """\
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                      package="${packageDotted}">
            
                <application>
                    <activity android:name=".${mainActivity}">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN"/>
            
                            <category android:name="android.intent.category.LAUNCHER"/>
                        </intent-filter>
                    </activity>
                </application>
            
            </manifest>
            """.stripIndent()
    writeActivity(mainActivity, packageDotted, baseDir)
  }

  private copyBuildsystemDir(File baseDir = getProjectDir()) {
    def rootBuildSystemDir = new File("$rootPath/buildsystem")
    def projectBuildSystemDir = new File("$baseDir/buildsystem")

    FileUtils.copyDirectory(rootBuildSystemDir, projectBuildSystemDir)
  }

  private void writeActivity(String name, String packageDotted, File baseDir = getProjectDir()) {
    def path = 'src/main/java/' + packageDotted.replace('.', '/') + "/${name}.java"
    def javaFile = createFile(path, baseDir)
    javaFile << """\
            package ${packageDotted};

            import android.app.Activity;
            import android.content.Intent;
            import android.os.Bundle;
            
            public class ${name} extends Activity {
            
                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                    startActivity(new Intent(this, ${name}.class));
                }
            }
            """.stripIndent()
  }
}
