# SoundCloud Android

## Building

Make sure the [Android SDK][] and Maven are installed:

    $ brew install android-sdk  # OSX
    $ mvn -version

Clone and build it:

    $ git clone git@github.com:soundcloud/SoundCloud-Android.git
    $ cd SoundCloud-Android
    $ mvn install -DskipTests
    $ adb install app/target/soundcloud-android-X.Y-SNAPSHOT.apk

If you don't want to use maven (who does?!) and have [sbt][] installed:

    $ sbt android:package-debug

## Handling dependencies / pom.xml

Dependencies should not be included in the repo, they are declared in the sbt
build file `project/build.scala`, split in `coreDependencies` and `testDependencies`.

Based on `build.scala` you can regenerate the `pom.xml` using `sbt mavenize`. To
actually download the dependencies to your working directory use `mvn
process-resources -U`, this will populate `app/lib` and `tests/lib`.

## Betas and releasing

Documented on the wiki: [releasing][], [betas][].

## Coding conventions / guidelines

Documented on the [wiki][].

[Android SDK]: http://developer.android.com/sdk/index.html
[sbt]: https://github.com/harrah/xsbt/
[wiki]: https://github.com/soundcloud/SoundCloud-Android/wiki/
[releasing]: https://github.com/soundcloud/SoundCloud-Android/wiki/Releasing
[betas]: https://github.com/soundcloud/SoundCloud-Android/wiki/Betas
