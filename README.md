# SoundCloud Android

## Building

Make sure the [Android SDK][], [Android NDK][] and Maven are installed:

    $ brew install android-sdk android-ndk # OSX - you'll also need XCode CLI tools
    $ mvn -version

Add thess lines to your .zshrc (or bash or whatever) [version numbers may change]

    export ANDROID_HOME=/usr/local/Cellar/android-sdk/r20
    export ANDROID_SDK_ROOT=/usr/local/Cellar/android-sdk/r20
    export ANDROID_SDK_HOME=/usr/local/Cellar/android-sdk/r20
    export ANDROID_NDK_HOME=/usr/local/Cellar/android-ndk/r7b/

Run

    $ android update sdk --no-ui --obsolete --force

Clone and build it:

    $ git clone git@github.com:soundcloud/SoundCloud-Android.git
    $ cd SoundCloud-Android
    $ git submodule init && git submodule update
    $ mvn install -DskipTests
    $ adb install app/target/soundcloud-android-X.Y-SNAPSHOT.apk

If you don't want to use maven (who does?!) and have [sbt][] installed:

    $ sbt android:package-debug

## Opening the project in Intellij IDEA

First make sure there are no leftover config files in the project (`find . -name '*.iml' | xargs rm && rm -rf .idea`).
Open IntelliJ, select "New Project", then "Import project from external model", select "Maven".
Make sure the settings look like in this screenshot: http://bit.ly/intellij-maven

Select Next and confirm the import of the parent project.

IDEA will automatically download and manage all dependencies. When switching branches you might need to reimport
the Maven project.

## Betas and releasing

Documented on the wiki: [releasing][], [betas][].

## Coding conventions / guidelines

Documented on the [wiki][].

[Android SDK]: http://developer.android.com/sdk/index.html
[Android NDK]: http://developer.android.com/sdk/ndk/index.html
[sbt]: https://github.com/harrah/xsbt/
[wiki]: https://github.com/soundcloud/SoundCloud-Android/wiki/
[releasing]: https://github.com/soundcloud/SoundCloud-Android/wiki/Releasing
[betas]: https://github.com/soundcloud/SoundCloud-Android/wiki/Betas
