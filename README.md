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

## Opening the project in Android Studio

Open Android Studio, select "Import project", select `build.gradle` from the root project directory.

Select Next and confirm the import of the parent project. In case you are asked to use the `gradle wrapper`, just say Yes.

Android Studio will automatically download and manage dependencies and will ask you to reload the project. 
    
## Running tests

### Robolectric tests on command line

You can run all or individual unit tests using Gradle. `cd` into the parent module, then run

    $ ./gradlew test

to run all tests, or

    $ ./gradlew test -Dtest.single=TrackingApiTest

to run all test inside a class, or

    $ ./gradlew test -Dtest.single=TrackingApiTest
    
to run one single test.

### Robolectric tests in Android Studio

Add a file `local.properties` to the app directory containing the path to the Android SDK:

    $ echo "sdk.dir=$ANDROID_HOME" > app/local.properties

In your Run/Debug configuration you should have one called: `robolectric-tests` which will execute all unit tests.

You should also setup your default run configuration for JUnit so it looks like this:
![JUnit default run config][JUnit default run config]

## [Wiki][wiki] topics

* [Integration tests][integration-tests]
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
[integration-tests]: https://github.com/soundcloud/SoundCloud-Android/wiki/Integration-tests
[android-guide]: https://github.com/soundcloud/SoundCloud-Android/wiki/Android-Guidelines
[java-syntax]: https://github.com/soundcloud/SoundCloud-Android/wiki/Java-Syntax-Conventions
[JUnit default run config]: http://f.cl.ly/items/3q3m3v2U0C1b0w1c2D2G/default_junit_run_configuration.png

