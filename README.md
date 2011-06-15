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

## Releasing

  * Make sure build is green (cf [Builder][])
  * Increase `versionCode` and set `versionName` in `AndroidManifest.xml`
  * Create a new crash form & change the form key in `SoundCloudApplication`
  * Tag the current version (`git tag -a 1.3.2`)
  * Do a quick sanity check diff from the previous released version (`git diff 1.x.y..1.3.2`)
  * Make sure you've got the keystore in `PROJECT_ROOT/soundcloud_sign`
  * Build and sign: `mvn install -Psign -Djarsigner.storepass=....` (prefix
  command with space to skip history)
  * Upload `target/soundcloud-android-1.x.y-aligned.apk` to the market

[Android SDK]: http://developer.android.com/sdk/index.html
[Builder]: http://builder.soundcloud.com/view/Android/job/soundcloud-android/
