# SoundCloud Android

## Building

Make sure the [Android SDK][] and Maven are installed:

    $ brew install android-sdk  # OSX
    $ mvn -version

Clone and build it:

    $ git clone git@github.com:soundcloud/SoundCloud-Android.git
    $ cd SoundCloud-Android
    $ mvn install
    $ adb install target/soundcloud-android-X.Y-SNAPSHOT.apk

If you don't want to use maven (who does?!) and have [sbt][] installed:

    $ sbt android:package-debug

## Handling dependencies / pom.xml

Dependencies should not be included in the repo, they are declared in the sbt
build file `project/build.scala`, split in `coreDependencies` and `testDependencies`.

Based on `build.scala` you can regenerate the `pom.xml` using `sbt mavenize`. To
actually download the dependencies to your working directory use `mvn
process-resources -U`, this will populate `lib/` and `tests/lib`.

## Releasing (Android market)

  * Make sure build is green (cf [Builder][])
  * For major releases - install previous market version and test upgrade process
  * Increase `versionCode` and set `versionName` in `AndroidManifest.xml`
  * Document changes in [changelog.txt][] and change the release date
  * Tag the current version (`rake release:tag`)
  * Do a quick sanity check diff from the previous released version (e.g. `git diff 1.3.1..1.3.2`)
  * Make sure you've got the keystore in `PROJECT_ROOT/soundcloud_sign`
  * Build and sign: `sbt clean android:prepare-market`
  * Upload `target/soundcloud-android-x.y.z-market.apk` to the market
  * Important: upload apk to github for archival: `sbt android:github-upload`
  * Release a beta with the same version code used in the release process

## Releasing (Amazon Appstore)

Amazon requires an unsigned apk - they then add some DRM/monitoring/lolcat code to the binary
and require us to sign the processed apk with our private key.

To create an unsigned apk:

    $ sbt android:package-release
    ...
    [info] Packaging /Users/jan/projects/soundcloud-android/target/soundcloud-android-2.2.5.apk

The processed apk from Amazon needs to be signed and zipaligned before uploading to Amazon.
Copy apk received from Amzazon to `target/soundcloud-android-x.y.z.apk`, then run:

    $ sbt android:prepare-amazon
    [info] Aligned /Users/jan/projects/soundcloud-android/target/soundcloud-android-2.2.5-market.apk
    [info] Signed /Users/jan/projects/soundcloud-android/target/soundcloud-android-2.2.5.apk

`soundcloud-android-x.y.z-market.apk` can now be uploaded to the Appstore.

## Releasing betas

  * Change `versionName` in AndroidManifest.xml (but *not* `versionCode`)
  * Build the apk: `rake beta:build`
  * Tag the current beta version: `rake beta:tag`
  * Upload to S3: `rake beta:upload`

## Integration from other Android apps

This is document elsewhere:

  * [Token Sharing][]
  * [Intent Sharing][]


[Android SDK]: http://developer.android.com/sdk/index.html
[Builder]: http://builder.soundcloud.com/view/Android/job/soundcloud-android/
[changelog.txt]: https://github.com/soundcloud/SoundCloud-Android/blob/master/res/raw/changelog.txt
[Token Sharing]: https://github.com/soundcloud/android-token-sharing
[Intent Sharing]: https://github.com/soundcloud/android-intent-sharing
[sbt]: https://github.com/harrah/xsbt/
