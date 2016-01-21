# SoundCloud Android

First read [CONTRIBUTING.md](CONTRIBUTING.md) if you want to make a change to production.

## Building

Prerequisites:

* (Mac only) Xcode command line tools
* JDK 8
  Refer to [Free Java Download][]<br>
  You can use [jenv][] to manage your environments.<br>
  Alternatively, you can set the `JAVA_HOME` environment variable:<br>
	`export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)`

### Install the [Android SDK][] and [Android Studio][]

**On Mac OS X:**

1. Install the following packages:

    `$ brew install android-sdk android-ndk maven`
    
2. Add the following lines to your shell's start-up script (e.g. .bash_profile, .zshrc):

    ```
    ANDROID_HOME=/usr/local/opt/android-sdk/ #Or another location`
    export ANDROID_HOME=$ANDROID_HOME
    export ANDROID_SDK_ROOT=$ANDROID_HOME
    export ANDROID_SDK_HOME=$ANDROID_HOME
    ```

3. Run the [Android SDK Manager][] to install packages:

    `$ android`
    
4. Install [Android Studio][].

**On Linux:**

1. Install [Android Studio][], which contains [Android SDK][].
2. Add the follwing lines to your shell's start-up script (e.g. .bash_profile, .zshrc):

    ```
    ANDROID_HOME=/usr/local/opt/android-sdk/ #Or another location
    export ANDROID_HOME=$ANDROID_HOME
    export ANDROID_SDK_ROOT=$ANDROID_HOME
    export ANDROID_SDK_HOME=$ANDROID_HOME
    ```
    
### Continue with the setup:

1. From within Android Studio, go to `Tools` &rarr; `Android` &rarr; `SDK Manager`.
2. From the `SDK Tools` tab, install the latest versions:<br>
   * `Android SDK Tools`<br>
   * `Android SDK Platform-Tools`<br>Refer to the `androidBuildToolsVersion` variable in [buildsystem/dependencies.gradle](buildsystem/dependencies.gradle).<br>
   * `Android SDK Build-Tools`<br>
3. Install the targetted release, based on the API level.<br>Refer to the `android:targetSdkVersion` variable in [AndroidManifest.xml](app/AndroidManifest.xml).
4. From the `SDK Platforms` tab, select the checkbox `Show Package Details`.
5. Unselect all system images.<br>You will use [Genymotion][] rather than Android Studio to manage the emulators.
6. From the SDK Manager, click `Launch Standalone SDK Manager` and scroll down to the `Extras` menu item.
7. Install the latest versions:
   * `Android Support Repository`
   * `Android Support Library`
   * `Google Play services`
   * `Google Repository`
   
   To avoid installing extra packages other than those listed, select the `Reject` radio button.

If you need to test against other Android Release versions, you can return to the Android SDK Manager later.

### Clone and build the project

Make sure you are on the VPN:

    $ git clone git@github.com:soundcloud/SoundCloud-Android.git
    $ cd SoundCloud-Android
    $ ./gradlew assembleDevDebug

If you encounter problems, check and update the [troubleshooting page](https://github.com/soundcloud/SoundCloud-Android/wiki/Troubleshooting).

You can also ask questions on the `#android-newbies` Slack channel.

## Open the project in Android Studio

1. From within Android Studio, select `File` &rarr; `New` &rarr; `Import project`, and select `build.gradle` from the root project directory.<br>(If you see the message, `Unregistered VCS root detected`, and you are not sure what to do, click `Add root`.)
2. Select `Next` and confirm the import of the parent project.<br>Android Studio automatically downloads and manages dependencies.<br>(If you are asked to use the `gradle wrapper`, select `Yes`.)<br>
3. After the download completes, click <code>Install <i>n</i> packages...</code>.<br>The installation might take several minutes to complete.
4. Reload the project.

## Set up the SoundCloud code style

1  Run `./gradlew setupCodeStyle`; this will install our code formatter to all your IntelliJs
1. From within Android Studio, go to `File` &rarr; `Other Settings` &rarr; `Default Settings` &rarr; `Editor` &rarr; `Code Style`.
1. From the `Scheme` drop-down menu, select `SoundCloud-Android`.

![Android code style][Android code style]

## Running the app on Genymotion

1. Install [Genymotion][].<br>If you see a similar error message, follow its instructions to resolve it: ![screenshot from 2015-10-23 09 31 09](https://cloud.githubusercontent.com/assets/1639324/10687056/705aec32-796a-11e5-85f2-d228e5bf0b6f.png)
2. Add a virtual device. A Google Nexus phone is a good one, eg. Google Nexus 6.
Click `Play` to start the device.

Install the Genymotion plugin for Android Studio in Preferences -> Plugins and search for Genymotion.

Click the Play button in Android Studio (next to app at the top) and it should recognize your Genymotino device.

## Running tests

**Note:** Read the [Wiki](https://github.com/soundcloud/SoundCloud-Android/wiki) pages on testing first!

### Unit tests on command line

You can run all or individual unit tests using Gradle. `cd` into the parent module, then run

    $ ./gradlew runUnitTests

to run all tests, or

    $ ./gradlew runUnitTests --tests *SimpleTrackingApiTest

to run all tests inside a class, or

    $ ./gradlew runUnitTests --tests *SimpleTrackingApiTest.failedTest

to run one single test.

**NOTE: for the legacy tests in tests-robolectric to run, you need to install the Android SDK platform v22 via the SDK manager**

### Unit tests in Android Studio

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

