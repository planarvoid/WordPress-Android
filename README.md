# SoundCloud Android

## Building

Make sure the [Android SDK][], [Android NDK][] and Maven (3.0.4+ required) are installed:

    $ brew install android-sdk android-ndk maven # OSX - you'll also need XCode CLI tools
    $ mv /usr/bin/mvn{,.old}
    $ mvn -version
    Apache Maven 3.0.4 (r1232337; 2012-01-17 09:44:56+0100)

Add thess lines to your .zshrc (or bash or whatever) [version numbers may change]

    export ANDROID_HOME=/usr/local/Cellar/android-sdk/r20
    export ANDROID_SDK_ROOT=/usr/local/Cellar/android-sdk/r20
    export ANDROID_SDK_HOME=/usr/local/Cellar/android-sdk/r20
    export ANDROID_NDK_HOME=/usr/local/Cellar/android-ndk/r7b/

Run

    $ android update sdk --no-ui --all --force

Clone and build it:

    $ git clone git@github.com:soundcloud/SoundCloud-Android.git
    $ cd SoundCloud-Android
    $ git submodule init && git submodule update
    $ mvn install -DskipTests
    $ adb install app/target/soundcloud-android-X.Y-SNAPSHOT.apk

## Opening the project in Intellij IDEA

First make sure there are no leftover config files in the project
(`git clean -df` or `(find . -name '*.iml' -print0 | xargs -0 rm) && rm -rf .idea`).
Open IntelliJ, select "New Project", then "Import project from external model", select "Maven".
Make sure the settings look like in this screenshot: http://bit.ly/intellij-maven

Select Next and confirm the import of the parent project.

IDEA will automatically download and manage all dependencies. When switching branches you might need to reimport
the Maven project.

## Running tests

### Robolectric tests on command line

You can run all or individual unit tests using Maven. `cd` into the parent module, then run

    $ mvn test -DfailIfNoTests=false

to run all tests, or

    $ mvn test -DfailIfNoTests=false -Dtest=FooTest,BarTest,BazTest#shouldHonk

to run individual tests.

### Robolectric tests in IDEA

Change the default JUnit Run/Debug configuration to look like this:
![JUnit default run config][JUnit default run config]

Add a file `local.properties` to the app directory containing the path to the Android SDK:

    $ echo "sdk.dir=/usr/local/Cellar/android-sdk/r20" > app/local.properties

## Integration tests

Documented on the wiki: [integration-tests][].

## Betas and releasing

Documented on the wiki: [releasing][], [betas][].

## Coding conventions / guidelines

Documented on the [wiki][].

[Android SDK]: http://developer.android.com/sdk/index.html
[Android NDK]: http://developer.android.com/sdk/ndk/index.html
[wiki]: https://github.com/soundcloud/SoundCloud-Android/wiki/
[releasing]: https://github.com/soundcloud/SoundCloud-Android/wiki/Releasing
[betas]: https://github.com/soundcloud/SoundCloud-Android/wiki/Betas
[integration-tests]: https://github.com/soundcloud/SoundCloud-Android/wiki/Integration-tests
[JUnit default run config]: http://f.cl.ly/items/153m2C2d001j0Y1L1K02/Screen%20Shot%202012-11-27%20at%2012.57.25%20PM.png
