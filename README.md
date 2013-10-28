# SoundCloud Android

## Building

Make sure the [Android SDK][] and Maven (3.0.4+ required) are installed.
Beware! You need Maven > 3.0.4 but < 3.1.0 due to 
[this issue](https://code.google.com/p/appengine-maven-plugin/issues/detail?id=31) breaking the build

    $ brew tap homebrew/versions
    $ brew install android-sdk android-ndk homebrew/versions/maven30 # OSX - you'll also need XCode CLI tools
    $ mv /usr/bin/mvn{,.old}
    $ mvn -version
    Apache Maven 3.0.4 (r1232337; 2012-01-17 09:44:56+0100)


Add thess lines to your .zshrc (or bash or whatever) [version numbers may change]

    export ANDROID_HOME=/usr/local/Cellar/android-sdk/r20
    export ANDROID_SDK_ROOT=/usr/local/Cellar/android-sdk/r20
    export ANDROID_SDK_HOME=/usr/local/Cellar/android-sdk/r20

Run

    $ android update sdk --no-ui --all --force

Clone and build it:

    $ git clone git@github.com:soundcloud/SoundCloud-Android.git
    $ cd SoundCloud-Android
    $ rake debug:build
    $ adb install app/target/soundcloud-android-X.Y-SNAPSHOT.apk

## Opening the project in Intellij IDEA

Make sure there are no leftover config files in the project (`git clean -dfx`).

Copy the current debug configuration into the app resource dir:

    $ cp app/properties/app_properties_debug.xml app/res/values/

Open IntelliJ, select "New Project", then "Import project", select `pom.xml` from the root project directory.

Select Next and confirm the import of the parent project.

IDEA will automatically download and manage dependencies. Due to a bug in IntelliJ you'll need to add all apklib 
modules to the "test" module.

![Test modules][Test modules]

## Running tests

### Robolectric tests on command line

You can run all or individual unit tests using Maven. `cd` into the parent module, then run

    $ mvn test -DfailIfNoTests=false

to run all tests, or

    $ mvn test -DfailIfNoTests=false -Dtest=FooTest,BarTest,BazTest#shouldHonk

to run individual tests.

### Robolectric tests in IDEA

Add a file `local.properties` to the app directory containing the path to the Android SDK:

    $ echo "sdk.dir=$ANDROID_HOME" > app/local.properties

Change the default JUnit Run/Debug configuration to look like this:
![JUnit default run config][JUnit default run config]

## Integration tests

Documented on the wiki: [integration-tests][].

## Betas and releasing

Documented on the wiki: [releasing][], [betas][].

## Coding conventions / guidelines

Documented on the [wiki][].

[Android SDK]: http://developer.android.com/sdk/index.html
[wiki]: https://github.com/soundcloud/SoundCloud-Android/wiki/
[releasing]: https://github.com/soundcloud/SoundCloud-Android/wiki/Releasing
[betas]: https://github.com/soundcloud/SoundCloud-Android/wiki/Betas
[integration-tests]: https://github.com/soundcloud/SoundCloud-Android/wiki/Integration-tests
[Test modules]: http://f.cl.ly/items/1B3Q3T1F0v2T233w3938/Screen%20Shot%202013-10-28%20at%201.27.16%20PM.png
[JUnit default run config]: http://f.cl.ly/items/153m2C2d001j0Y1L1K02/Screen%20Shot%202012-11-27%20at%2012.57.25%20PM.png

