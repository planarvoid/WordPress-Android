package com.soundcloud.android.lint

import nebula.test.IntegrationSpec

class LibraryIntegrationSpec extends IntegrationSpec {

  void setup() {
    settingsFile << settingsGradle()
    buildFile << androidBuild("com.example.android")
    writeAndroidManifestWithActivity("com.example.android", "MainActivity")

    addSubproject(":modules:android-kit:platform", "")
    addSubproject(":modules:android-kit:core", "")
    addSubproject(":modules:groupie:groupie-compiler", "")
    addSubproject(":modules:groupie:groupie", "")
    addSubproject(":modules:lint:lib", "")
    addSubproject(":modules:mrlocallocal", "")
  }

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
              include ':lib', ':modules:lint:rules', ':integTest'
              project(':lib').projectDir = file('../../../../../lib')
              project(':modules:lint:rules').projectDir = file('../../../../../rules')
           """.stripIndent()
  }

  protected String androidBuild(String applicationId) {
    return """\
            apply from: '../../../../../../../buildsystem/dependencies.gradle'
            apply plugin: 'com.android.application'

            buildscript {
                apply from: '../../../../../../../buildsystem/dependencies.gradle'
                def gradlePlugins = rootProject.ext.gradlePlugins
                
                repositories {
                    jcenter()
                    mavenCentral()
                }
                dependencies {
                    classpath gradlePlugins.android
                }
            }
            
            repositories {
                jcenter()
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
                    compile project(":lib")
                }
                lintOptions {
                    check 'sc.StartIntent', 'sc.CreateIntent'
                    textReport true
                    textOutput 'stdout'
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
