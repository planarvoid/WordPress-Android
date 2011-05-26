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

[Android SDK]: http://developer.android.com/sdk/index.html
