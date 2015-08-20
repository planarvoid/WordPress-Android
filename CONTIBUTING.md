# Expectations

Running on a feature team model means there are different expecations when you contribute code to the Android app. Each team is independently accountable for their features. At the same time, there are responsibilities to the other teams contributing to the app.

## Share your code early

There are two ways to do this:
* work on a branch and open up a WIP PR on github
* make tested, working commits to master, and push them frequently

If you are pairing you can choose whichever suits. If you are working solo, you need to use a PR.

## Communicate early with Release Captains

If you want to get a feature or bugfix into the next beta, contact the release
captains and make them aware of your intention. They can advise you of
timelines and help you in case your change is urgent.

You can find out who the current release captains are in the #android slack channel–look at the topic.

Make sure you are subscribed to [android-releases@soundcloud.com](https://groups.google.com/a/soundcloud.com/forum/#!forum/android-releases).
Information about release timelines and contents are sent here.

## Assure your feature's quality

* have someone on your feature team test changes you make
* regression test your area of the app, when approaching a release
* automate more acceptance tests so you have less regression testing to do

## Monitor your feature in production

* build and monitor your own feature specific prometheus metrics
* check shared dashboards such as Fabric for items related to your feature
