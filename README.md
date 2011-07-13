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
  * Document changes in [changelog.txt][] and add the release date
  * Tag the current version (`git tag -a 1.3.2`)
  * Do a quick sanity check diff from the previous released version (`git diff 1.x.y..1.3.2`)
  * Make sure you've got the keystore in `PROJECT_ROOT/soundcloud_sign`
  * Build and sign: `mvn install -Psign -DskipTests -Djarsigner.storepass=....` (prefix
  command with space to skip history)
  * For major releases - install previous market version and test upgrade
  process
  * Upload `target/soundcloud-android-1.x.y-market.apk` to the market

## Releasing betas

  * Change `versionName` in AndroidManifest.xml (but *not* `versionCode`)
  * Create a tag with the versionName you used in step 1)
  * Build the apk: `rake beta:build`
  * Upload to S3: `rake beta:upload`

## Integration from other Android apps

This is document elsewhere:

  * [Token Sharing][]
  * [Intent Sharing][]


[Android SDK]: http://developer.android.com/sdk/index.html
[Builder]: http://builder.soundcloud.com/view/Android/job/soundcloud-android/
[changelog.txt]: https://github.com/soundcloud/SoundCloud-Android/blob/master/res/raw/changelog.txt]
[Token Sharing]: https://github.com/soundcloud/android-token-sharing
[Intent Sharing]: https://github.com/soundcloud/android-intent-sharing
