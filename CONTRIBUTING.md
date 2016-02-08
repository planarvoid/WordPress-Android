# Expectations

Running on a feature team model means there are different expecations when you contribute code to the Android app. Each team is independently accountable for their features. At the same time, there are responsibilities to the other teams contributing to the app.

## Share your code early

1. Work on a branch and open up a `WIP` PR on GitHub.
1. Make tested and working commits, and push them frequently.
1. When your changes are ready for final review, replace the `WIP` label with `AWAITING REVIEW`. Team members are expected to regularly visit the open PRs page and review those flagged as awaiting a review.
1. Wait for two thumbs up for your PR before you merge it. Alternatively, wait for one thumb up if you paired on your PR.

## Make it simple for others to review your code

Respect the time others have to put into reviewing your code. Aim for small and digestible PRs; as a rule of thumb, PRs should result in diffs with *at most* ~500 changes. A good practice to arrive at smaller increments is to think about how to break up stories early on. *It is perfectly fine to PR partial results!* For instance, a story that touches on networking, business logic and presentation could be broken up into 3 PRs, where each one provides stubbed out integration points to the next one, only coming together to the final solution with the last PR. Feature toggles can help you not exposing partially integrated code to other developers or even users.

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
