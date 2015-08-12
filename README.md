# SoundCloud Android

## Building

Make sure both [Android SDK][] and [Android Studio][] are installed.

    $ brew tap homebrew/versions
    $ brew install android-sdk android-ndk homebrew/versions/maven # OSX - you'll also need XCode CLI tools
    

Add these lines to your shell's startup script (e.g. .bash_profile, .zshrc)

    ANDROID_HOME=/usr/local/opt/android-sdk/
    export ANDROID_HOME=$ANDROID_HOME
    export ANDROID_SDK_ROOT=$ANDROID_HOME
    export ANDROID_SDK_HOME=$ANDROID_HOME

Make sure you are using JDK 8:

    export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
    
or you can use [Jenv][] as your Java Environment Manager.
    

Run

    $ android update sdk --no-ui --all --force

Clone and build this project:

    $ git clone git@github.com:soundcloud/SoundCloud-Android.git
    $ cd SoundCloud-Android
    $ ./gradlew assembleDebug
    $ ./gradlew installDebug

If you encounter problems, check (and update) the [troubleshooting page](https://github.com/soundcloud/SoundCloud-Android/wiki/Troubleshooting).

## Opening the project in Android Studio

Open Android Studio, select "Import project", select `build.gradle` from the root project directory.

Select Next and confirm the import of the parent project. In case you are asked to use the `gradle wrapper`, just say Yes.

Android Studio will automatically download and manage dependencies and will ask you to reload the project. 

## Setup code style

Make sure you are using SoundCloud code style on Android Studio by going to: 
File -> Other Settings -> Default Settings -> Code Style and apply: `SoundCloud-Android` scheme.

If it doesn't appear in the list, try the following. Tailor the path for your version of AndroidStudio. The link source MUST be an absolute path.

    $ mkdir ~/Library/Preferences/AndroidStudio1.2/codestyles
    $ ln -sf ~/sc/SoundCloud-Android/.idea-codestyle.xml ~/Library/Preferences/AndroidStudio1.2/codestyles/SoundCloud-Android.xml

![Android code style][Android code style]
    
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

![JUnit default run config][JUnit default run config]

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

[Android SDK]: http://developer.android.com/sdk/index.html
[Android Studio]: http://developer.android.com/sdk/index.html
[Jenv]: http://www.jenv.be/
[wiki]: https://github.com/soundcloud/SoundCloud-Android/wiki/
[releasing]: https://github.com/soundcloud/SoundCloud-Android/wiki/Releasing
[betas]: https://github.com/soundcloud/SoundCloud-Android/wiki/Betas
[acceptance-tests]: https://github.com/soundcloud/SoundCloud-Android/wiki/Acceptance-Tests
[android-guide]: https://github.com/soundcloud/SoundCloud-Android/wiki/Android-Guidelines
[java-syntax]: https://github.com/soundcloud/SoundCloud-Android/wiki/Java-Syntax-Conventions
[JUnit default run config]: http://f.cl.ly/items/3q3m3v2U0C1b0w1c2D2G/default_junit_run_configuration.png
[Android code style]: http://f.cl.ly/items/1j0U3Q0i330b3G2D3G1D/codestye_soundcloud.png

