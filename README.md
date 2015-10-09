# SoundCloud Android

First read [CONTRIBUTING.md](CONTRIBUTING.md) if you want to make a change to production.

## Building

Prerequisites:

* (Mac only) Xcode command line tools
* GCC (`gcc`) and GNU Make (`make`)
* Java 7 or higher<br>
  Refer to [Free Java Download][]<br>
  You can use [jenv][] to manage your environments.<br>
  Alternatively, you can set the `JAVA_HOME` environment variable:<br>
	`export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)`

### Install the [Android SDK][] and [Android Studio][]

#### On Mac OS X:

    $ brew install android-sdk android-ndk maven

Add these lines to your shell's startup script (e.g. .bash_profile, .zshrc)

    ANDROID_HOME=/usr/local/opt/android-sdk/
    export ANDROID_HOME=$ANDROID_HOME
    export ANDROID_SDK_ROOT=$ANDROID_HOME
    export ANDROID_SDK_HOME=$ANDROID_HOME

Run the [Android SDK Manager][] to install packages.

    $ android

You don't need to install everything. To get started, you can install the following:

* From `Tools`, install the latest versions of `Android SDK Tools` and `Android SDK Platform-tools`
and the version of `Android SDK Build-tools` specified by the `androidBuildToolsVersion` variable in
[buildsystem/dependencies.gradle](buildsystem/dependencies.gradle).
* Install the release we are targeting, which is currently `Android 5.0.1 (API 21)`. You can check by
looking for `android:targetSdkVersion` in [AndroidManifest.xml](app/AndroidManifest.xml). Install
all release packages except for the system images, because we will use [Genymotion][] for managing emulators.
* From `Extras`, install the latest versions of `Android Support Repository`, `Android Support Library`,
`Google Play services` and `Google Repository`.

If you need to test against other Android Release versions, you can return to the Android SDK Manager later.

Install [Android Studio][].

#### On Linux:

Install [Android Studio][], which contains [Android SDK][].

### Clone and build the project

Make sure you are on the VPN:

    $ git clone git@github.com:soundcloud/SoundCloud-Android.git
    $ cd SoundCloud-Android
    $ ./gradlew assembleDebug

You might encounter the following error:

```
Parallel execution with configuration on demand is an incubating feature.

FAILURE: Build failed with an exception.

* What went wrong:
A problem occurred configuring project ':app'.
> SDK location not found. Define location with sdk.dir in the local.properties file or with an ANDROID_HOME environment variable.

* Try:
Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output.

BUILD FAILED

Total time: 1.753 secs
```

If you encounter problems, check and update the [troubleshooting page](https://github.com/soundcloud/SoundCloud-Android/wiki/Troubleshooting).

## Opening the project in Android Studio

Open Android Studio, select `File` > `New` > `Import project`, and select `build.gradle` from the root project directory.

Select Next and confirm the import of the parent project. In case you are asked to use the `gradle wrapper`, just say Yes.

Android Studio will automatically download and manage dependencies. When that download is complete, click <code>Install <i>n</i> packages...</code>. Then, reload the project.

## Setup code style

Make sure you are using SoundCloud code style on Android Studio by going to:
File -> Other Settings -> Default Settings -> Code Style and apply: `SoundCloud-Android` scheme.

If it doesn't appear in the list, try the following. Tailor the path for your version of AndroidStudio. The link source MUST be an absolute path.

    $ mkdir ~/Library/Preferences/AndroidStudio1.2/codestyles
    $ ln -sf ~/sc/SoundCloud-Android/.idea-codestyle.xml ~/Library/Preferences/AndroidStudio1.2/codestyles/SoundCloud-Android.xml

![Android code style][Android code style]

## Running the app on Genymotion

Install [Genymotion][] and add a virtual device. A Google Nexus phone is a good one, eg. Google Nexus 6.
Click Play to start the device.

Install the Genymotion plugin for Android Studio in Preferences -> Plugins and search for Genymotion.

Click the Play button in Android Studio (next to app at the top) and it should recognize your Genymotino device.

## Running tests

### Robolectric tests on command line

You can run all or individual unit tests using Gradle. `cd` into the parent module, then run

    $ ./gradlew test

to run all tests, or

    $ ./gradlew tests-robolectric:test --tests *SimpleTrackingApiTest

to run all tests inside a class, or

    $ ./gradlew tests-robolectric:test --tests *SimpleTrackingApiTest.failedTest

to run one single test.

### Robolectric tests in Android Studio

Add a file `local.properties` to the app directory containing the path to the Android SDK:

    $ echo "sdk.dir=$ANDROID_HOME" > app/local.properties

In your Run/Debug configuration you should have one called: `robolectric-tests` which will execute all unit tests.

You should also setup your default run configuration for JUnit so it looks like this:

![screen shot 2015-09-01 at 18 24 57](https://cloud.githubusercontent.com/assets/513206/9610004/22a788ee-50d7-11e5-8789-7ab7c50d60de.png)

## Useful Gradle commands

    $ ./gradlew clean               --> Performs a clean up of the whole project.
    $ ./gradlew tasks [--all]       --> Shows all available tasks for the project.
    $ ./gradlew staticAnalysis      --> Performs a full static analysis of the codebase.
    $ ./gradlew :app:dependencies   --> Shows a tree with all dependencies of the Android main app.

## [Wiki][wiki] topics

* [Acceptance Tests][acceptance-tests]
* [Releasing][releasing]
* [Betas][betas]
* [Android guidelines][android-guide]
* [Java syntax][java-syntax]

[Free Java Download]: http://java.com/en/download
[Android SDK]: http://developer.android.com/sdk/index.html
[Android SDK Manager]: http://developer.android.com/sdk/installing/adding-packages.html
[Android Studio]: http://developer.android.com/sdk/index.html
[Genymotion]: https://www.genymotion.com
[Jenv]: http://www.jenv.be/
[wiki]: https://github.com/soundcloud/SoundCloud-Android/wiki/
[releasing]: https://github.com/soundcloud/SoundCloud-Android/wiki/Releasing
[betas]: https://github.com/soundcloud/SoundCloud-Android/wiki/Betas
[acceptance-tests]: https://github.com/soundcloud/SoundCloud-Android/wiki/Acceptance-Tests
[android-guide]: https://github.com/soundcloud/SoundCloud-Android/wiki/Android-Guidelines
[java-syntax]: https://github.com/soundcloud/SoundCloud-Android/wiki/Java-Syntax-Conventions
[JUnit default run config]: http://f.cl.ly/items/3q3m3v2U0C1b0w1c2D2G/default_junit_run_configuration.png
[Android code style]: http://f.cl.ly/items/1j0U3Q0i330b3G2D3G1D/codestye_soundcloud.png

