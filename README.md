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

If you don't want to use maven (who does?!) and have [sbt][] installed:

    $ sbt android:package-debug

## Handling dependencies / pom.xml

Dependencies should not be included in the repo, they are declared in the sbt
build file `project/build.scala`, split in `coreDependencies`,
`providedDependencies` and `testDependencies`.

Based on `build.scala` you can generate the `pom.xml` using `sbt mavenize`. To
actually download the dependencies to your working directory use `mvn
process-resources`, this will populate `lib/` and `tests/lib`.

## Releasing

  * Make sure build is green (cf [Builder][])
  * For major releases - install previous market version and test upgrade process
  * Increase `versionCode` and set `versionName` in `AndroidManifest.xml`, set version in `pom.xml`
  * Document changes in [changelog.txt][] and change the release date
  * Tag the current version (`rake release:tag`)
  * Do a quick sanity check diff from the previous released version (`git diff 1.x.y..1.3.2`)
  * Make sure you've got the keystore in `PROJECT_ROOT/soundcloud_sign`
  * Build and sign: `mvn clean install -Psign -DskipTests -Djarsigner.storepass=....` (prefix
  command with space to skip history)
  * Upload `target/soundcloud-android-1.x.y-market.apk` to the market
  * Release a beta with the same version code used in the release process

## Releasing betas

  * Change `versionName` in AndroidManifest.xml (but *not* `versionCode`)
  * Build the apk: `rake beta:build`
  * Tag the current beta version: `rake beta:tag`
  * Upload to S3: `rake beta:upload`
  * Push notifications to beta clients: `rake beta:notify`

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
