# SoundCloud Android

## Building

Make sure the [Android SDK][], [Android NDK][] and Maven are installed:

    $ brew install android-sdk android-ndk # OSX - you'll also need XCode CLI tools
    $ mvn -version

Add thess lines to your .zshrc (or bash or whatever)

    export ANDROID_HOME=/usr/local/Cellar/android-sdk/r20
    export ANDROID_SDK_ROOT=/usr/local/Cellar/android-sdk/r20
    export ANDROID_SDK_HOME=/usr/local/Cellar/android-sdk/r20

Run

    $ /usr/local/Cellar/android-sdk/r20/tools/android update sdk --no-ui --obsolete --force

Clone and build it:

    $ git clone git@github.com:soundcloud/SoundCloud-Android.git
    $ cd SoundCloud-Android
    $ git submodule init && git submodule update
    $ mvn install -DskipTests
    $ adb install app/target/soundcloud-android-X.Y-SNAPSHOT.apk

If you don't want to use maven (who does?!) and have [sbt][] installed:

    $ sbt android:package-debug

## Handling dependencies / pom.xml

Dependencies should not be included in the repo, they are declared in the sbt
build file `project/build.scala`, split in `coreDependencies` and `testDependencies`.

Based on `build.scala` you can regenerate the `pom.xml` using `sbt mavenize`. To
actually download and copy the dependencies to your working directory use
`sbt android:copy-libs`, this will populate `lib/` and `tests/lib`.

To download dependencies for the integration tests:
`sbt 'project tests-integration' android:copy-libs'` (this will populate
`tests-integration/lib`).

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
