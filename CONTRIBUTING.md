# Expectations

Running on a feature team model means there are different expecations when you contribute code to the Android app. Each team is independently accountable for their features. At the same time, there are responsibilities to the other teams contributing to the app.

## Share your code early

1. Work on a branch and open up a WIP PR on GitHub.
2. Make tested and working commits, and push them frequently.
3. When your changes are ready for final review, remove `WIP` from the PR title and ask for a review in the `#android` slack channel.
4. Wait for two thumbs up for your PR before you merge it. Alternatively, wait for one thumb up if you paired on your PR.

## Communicate early with Release Captains

If you want to get a feature or bugfix into the next beta, contact the release
captains and make them aware of your intention. They can advise you of
timelines and help you in case your change is urgent.

You can find out who the current release captains are in the #android slack channel–look at the topic.

Make sure you are subscribed to [android-releases@soundcloud.com](https://groups.google.com/a/soundcloud.com/forum/#!forum/android-releases).
Information about release timelines and contents are sent here.

## Assure your feature's quality

* Have someone on your feature team test your changes.
* Regression test your area of the app, when approaching a release.
* Automate more acceptance tests so you have less regression testing to do.

## Monitor your feature in production

* Build and monitor your own feature specific Prometheus metrics.
* Check shared dashboards such as Fabric for items related to your feature.
