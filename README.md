# SoundCloud Android

## Building

Make sure the [Android SDK][] is installed:

    $ brew install android-sdk  # OSX

Clone and build it:

    $ git clone git@github.com:soundcloud/SoundCloud-Android.git
    $ cd SoundCloud-Android
    $ ant debug
    $ adb install bin/soundcloud-debug.apk

To run tests:

    $ cd SoundCloud-Android
    $ ant compile
    $ cd tests
    $ ant test

[Android SDK]: http://developer.android.com/sdk/index.html
