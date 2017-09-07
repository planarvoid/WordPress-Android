## Code formatting

Setup your code style by running `./gradlew setupCodeStyle` and use the created style.

## Languages

The android-listeners codebase uses both Java and Kotlin while Kotlin should replace Java over time. Our guidelines are:
- New code should be written in Kotlin
- Existing Java code can be converted to Kotlin as per the developer discretion
  - For a one line change it might not make sense converting a 1k+ LOC Java file
  - Be sensible about changes that might introduce bugs
  - Before converting a class think about the general architecture of the app and how this class fits into it. If needed, use the Kotlin conversion to rearchitect as part of a refactor ticket.
- Be comfortable with moving slower in the beginning.
- It is okay to have both Java and Kotlin in the same codebase, we don't have to convert everything

## Pull Requests

All changes should be merged through a Pull Request to `master`

Respect the time others have to put into reviewing your code. Aim for small and digestible PRs; as a rule of thumb, PRs should result in diffs with *at most* ~500 changes. A good practice to arrive at smaller increments is to think about how to break up stories early on. *It is perfectly fine to PR partial results!* For instance, a story that touches on networking, business logic and presentation could be broken up into 3 PRs, where each one provides stubbed out integration points to the next one, only coming together to the final solution with the last PR. Feature toggles can help you not exposing partially integrated code to other developers or even users.

### Share your code early

1. Work on a branch and open up a `WIP` PR on GitHub.
1. Make tested and working commits, and push them frequently.

### Review policy

1. Pull Requests with the label `AWAITING REVIEW` will be reviewed. Team members are expected to regularly visit the open PRs page and review those flagged as awaiting a review.
1. Wait for two thumbs up for your PR before you merge it. Alternatively, wait for one thumb up if you paired on your PR. A reviewed PR should carry the flag `REVIEWED` until it is merged.

## CI builds

1. Commenting `ci-build` on a Pull Request will kick off a PR build on our CI server 
1. Verify all stages pass:
    1. Compile - compiles your code
    1. Acceptance Tests - Runs instrumentation and UI tests (tests in the `/androidTest` and `/integrationTest` directories)
    1. Unit Tests - Runs all unit tests
    1. Static Analysis - Runs multiple static analysis suites on the codebase. Violations on the changeset of the PR will automatically be commented on the GitHub PR. Will go red on any finding with severity `ERROR` but all findings should be resolved.
    
If you wish to be notified on Slack about the result of your PR builds add your GitHub and Slack username to the [`./scripts/.slack`](https://github.com/soundcloud/android-listeners/blob/master/scripts/.slack) file. 
This will send you messages about the following topics in a private message on Slack:

- Messages about PR build results (success/failure)
- Notifications about Stale PRs

### Add animated gifs to PRs to visually demonstrate what you've done
 
Creating Gifs is a two step process: First, make a screen recording. Second, convert the recording to a gif.

We can also recommend using [RoboGif](https://github.com/izacus/RoboGif)

#### Creating a Screen Recording

##### From Genymotion

  1. Click the "Capture" icon.
  2. A dialog will open
  3. Click the "Screencast" Button to start a recording 

##### From the Android Emulator

```
adb shell screenrecord /sdcard/name-of-video.mp4
adb pull /sdcard/name-of-video.mp4
```

Or use the Screen Recording function of the `Android Profiler` in Android Studio.

#### Converting the gif

First, ensure you have `ffmpeg` installed. On mac:

`brew install ffmpeg`

Second, run the conversion script (found in the root directory for this repo):
 
`./movie-to-gif input-movie.webm output-image.gif`

## Communicate early with Release Captains

If you want to get a feature or bugfix into the next release, contact the release
captains and make them aware of your intention. They can advise you of
timelines and help you in case your change is urgent.

You can find out who the current release captains are in the #android slack channel–look at the topic.

To see upcoming code-freezes and the timing of releases see the [https://calendar.google.com/calendar/render?pli=1#details-sharing_5%7Cdtv-c291bmRjbG91ZC5jb21fMXE3ZjVxYjI1cjAydjVudTFnY3UydHQ3N2tAZ3JvdXAuY2FsZW5kYXIuZ29vZ2xlLmNvbQ-1-0](Mobile Release Train Calendar)

## Assure your feature's quality

* Have someone on your feature team test your changes.
* Regression test your area of the app, when approaching a release.
* Automate more acceptance tests so you have less regression testing to do.

## Monitor your feature in production

* Build and monitor your own feature specific Prometheus metrics.
* Check shared dashboards such as Fabric for items related to your feature.

