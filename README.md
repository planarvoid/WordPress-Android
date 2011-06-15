# SoundCloud Android

## Building

Make sure the [Android SDK][] and Maven are installed:

    $ brew install android-sdk  # OSX
    $ mvn -version

Clone and build it:

    $ git clone git@github.com:soundcloud/SoundCloud-Android.git
    $ cd SoundCloud-Android
    $ mvn install
    $ adb install target/soundcloud-android-1.X-SNAPSHOT.apk

How to release:

  * Increase `versionCode` and `versionName` in `AndroidManifest.xml`
  * Tag the current version (`git tag -a 1.3.2`)
  * Perform a clean build + sign (`ant clean release`), make sure all libs in
  `lib/` are uptodate (`rm -rf lib && mvn clean && mvn install`)

[Android SDK]: http://developer.android.com/sdk/index.html
