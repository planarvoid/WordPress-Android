package com.soundcloud.android.sync.activities;

import com.soundcloud.android.R;
import com.soundcloud.android.activities.ActivityProperty;
import com.soundcloud.java.collections.Property;
import com.soundcloud.java.collections.PropertySet;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

class NotificationMessage {
    public final CharSequence title, message, ticker;

    private NotificationMessage(CharSequence title, CharSequence message, CharSequence ticker) {
        this.title = title;
        this.message = message;
        this.ticker = ticker;
    }

    static class Builder {
        private final Resources res;
        private List<PropertySet> likes = Collections.emptyList();
        private List<PropertySet> comments = Collections.emptyList();
        private List<PropertySet> reposts = Collections.emptyList();
        private List<PropertySet> followers = Collections.emptyList();
        private List<PropertySet> allActivitiesToNotify = new LinkedList<>();

        Builder(Resources resources) {
            this.res = resources;
        }

        public Builder setLikes(List<PropertySet> likes) {
            this.likes = likes;
            this.allActivitiesToNotify.addAll(likes);
            return this;
        }

        public Builder setComments(List<PropertySet> comments) {
            this.comments = comments;
            this.allActivitiesToNotify.addAll(comments);
            return this;
        }

        public Builder setReposts(List<PropertySet> reposts) {
            this.reposts = reposts;
            this.allActivitiesToNotify.addAll(reposts);
            return this;
        }

        public Builder setFollowers(List<PropertySet> followers) {
            this.followers = followers;
            this.allActivitiesToNotify.addAll(followers);
            return this;
        }

        NotificationMessage build() {
            if (hasRepostsOnly()) {
                return buildRepostsOnlyNotification();
            } else if (hasLikesOnly()) {
                return buildLikesOnlyNotification();
            } else if (hasCommentsOnly()) {
                return buildCommentsOnlyNotification();
            } else if (hasFollowersOnly()) {
                return buildFollowersOnlyNotification();
            } else {
                return buildMixedActivitiesNotification();
            }
        }

        private boolean hasCommentsOnly() {
            return !comments.isEmpty() && likes.isEmpty() && reposts.isEmpty() && followers.isEmpty();
        }

        private boolean hasLikesOnly() {
            return !likes.isEmpty() && comments.isEmpty() && reposts.isEmpty() && followers.isEmpty();
        }

        private boolean hasRepostsOnly() {
            return !reposts.isEmpty() && likes.isEmpty() && comments.isEmpty() && followers.isEmpty();
        }

        private boolean hasFollowersOnly() {
            return !followers.isEmpty() && likes.isEmpty() && comments.isEmpty() && reposts.isEmpty();
        }

        private List<String> getUniqueStrings(Property<String> property) {
            final List<String> titles = new ArrayList<>();
            for (PropertySet activity : allActivitiesToNotify) {
                final String title = activity.getOrElseNull(property);
                if (title != null && !titles.contains(title)) {
                    titles.add(title);
                }
            }
            return titles;
        }

        private NotificationMessage buildMixedActivitiesNotification() {
            List<String> playableTitles = getUniqueStrings(ActivityProperty.PLAYABLE_TITLE);
            List<String> userNames = getUniqueStrings(ActivityProperty.USER_NAME);
            final CharSequence ticker = res.getQuantityString(R.plurals.dashboard_notifications_activity_ticker_activity,
                    allActivitiesToNotify.size(),
                    allActivitiesToNotify.size());

            final CharSequence title = res.getQuantityString(R.plurals.dashboard_notifications_activity_title_activity,
                    allActivitiesToNotify.size(),
                    allActivitiesToNotify.size());

            final CharSequence message;
            if (userNames.size() == 1) {
                if (playableTitles.size() == 1) {
                    message = res.getString(R.string.dashboard_notifications_activity_message_activity_one_user_one_playable,
                            userNames.get(0),
                            playableTitles.get(0));
                } else {
                    message = res.getString(R.string.dashboard_notifications_activity_message_activity_one_user_multiple_playables,
                            userNames.get(0));
                }
            } else if (userNames.size() == 2) {
                message = res.getString(R.string.dashboard_notifications_activity_message_activity_two,
                        userNames.get(0),
                        userNames.get(1));
            } else {
                message = res.getString(R.string.dashboard_notifications_activity_message_activity_other,
                        userNames.get(0),
                        userNames.get(1));
            }
            return new NotificationMessage(title, message, ticker);
        }

        private NotificationMessage buildCommentsOnlyNotification() {
            List<String> playableTitles = getUniqueStrings(ActivityProperty.PLAYABLE_TITLE);
            List<String> userNames = getUniqueStrings(ActivityProperty.USER_NAME);

            final CharSequence ticker = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_ticker_comment,
                    comments.size(),
                    comments.size());

            final CharSequence title = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_title_comment,
                    comments.size(),
                    comments.size());

            final CharSequence message;
            if (playableTitles.size() == 1) {
                if (comments.size() == 1) {
                    message = res.getString(
                            R.string.dashboard_notifications_activity_message_comment_single_track_one,
                            playableTitles.get(0),
                            comments.get(0).get(ActivityProperty.USER_NAME));
                } else if (comments.size() == 2) {
                    message = res.getString(
                            R.string.dashboard_notifications_activity_message_comment_single_track_two,
                            comments.size(),
                            playableTitles.get(0),
                            comments.get(0).get(ActivityProperty.USER_NAME),
                            comments.get(1).get(ActivityProperty.USER_NAME));
                } else {
                    message = res.getString(
                            R.string.dashboard_notifications_activity_message_comment_single_track_other,
                            comments.size(),
                            playableTitles.get(0),
                            comments.get(0).get(ActivityProperty.USER_NAME),
                            comments.get(1).get(ActivityProperty.USER_NAME));
                }
            } else {
                if (userNames.size() == 1) {
                    message = res.getString(R.string.dashboard_notifications_activity_message_comment_one,
                            userNames.get(0));
                } else if (userNames.size() == 2) {
                    message = res.getString(R.string.dashboard_notifications_activity_message_comment_two,
                            userNames.get(0),
                            userNames.get(1));
                } else {
                    message = res.getString(R.string.dashboard_notifications_activity_message_comment_other,
                            userNames.get(0),
                            userNames.get(1));
                }
            }
            return new NotificationMessage(title, message, ticker);
        }

        private NotificationMessage buildLikesOnlyNotification() {
            List<String> playableTitles = getUniqueStrings(ActivityProperty.PLAYABLE_TITLE);
            final CharSequence ticker = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_ticker_like,
                    likes.size(),
                    likes.size());

            final CharSequence title = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_title_like,
                    likes.size(),
                    likes.size());

            // Note: Transifex requires plurals to have numbers, otherwise they should be just strings
            final CharSequence message;
            if (playableTitles.size() == 1 && likes.size() == 1) {
                message = res.getString(R.string.dashboard_notifications_activity_message_likes,
                        likes.get(0).get(ActivityProperty.USER_NAME),
                        likes.get(0).get(ActivityProperty.PLAYABLE_TITLE));
            } else if (playableTitles.size() == 1) {
                message = res.getString(R.string.dashboard_notifications_activity_message_like_one,
                        playableTitles.get(0));
            } else if (playableTitles.size() == 2) {
                message = res.getString(R.string.dashboard_notifications_activity_message_like_two,
                        playableTitles.get(0),
                        playableTitles.get(1));
            } else {
                message = res.getString(R.string.dashboard_notifications_activity_message_like_other,
                        playableTitles.get(0),
                        playableTitles.get(1));
            }
            return new NotificationMessage(title, message, ticker);
        }

        private NotificationMessage buildRepostsOnlyNotification() {
            List<String> playableTitles = getUniqueStrings(ActivityProperty.PLAYABLE_TITLE);
            final CharSequence ticker = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_ticker_repost,
                    reposts.size(),
                    reposts.size());

            final CharSequence title = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_title_repost,
                    reposts.size(),
                    reposts.size());

            // Note: Transifex requires plurals to have numbers, otherwise they should be just strings
            final CharSequence message;
            if (playableTitles.size() == 1 && reposts.size() == 1) {
                message = res.getString(R.string.dashboard_notifications_activity_message_repost,
                        reposts.get(0).get(ActivityProperty.USER_NAME),
                        reposts.get(0).get(ActivityProperty.PLAYABLE_TITLE));
            } else if (playableTitles.size() == 1) {
                message = res.getString(R.string.dashboard_notifications_activity_message_repost_one,
                        playableTitles.get(0));
            } else if (playableTitles.size() == 2) {
                message = res.getString(R.string.dashboard_notifications_activity_message_repost_two,
                        playableTitles.get(0),
                        playableTitles.get(1));
            } else {
                message = res.getString(R.string.dashboard_notifications_activity_message_repost_other,
                        playableTitles.get(0),
                        playableTitles.get(1));
            }
            return new NotificationMessage(title, message, ticker);
        }

        private NotificationMessage buildFollowersOnlyNotification() {
            List<String> userNames = getUniqueStrings(ActivityProperty.USER_NAME);
            final CharSequence ticker = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_follower,
                    userNames.size(),
                    userNames.size());

            final CharSequence title = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_follower,
                    userNames.size(),
                    userNames.size());

            // Note: Transifex requires plurals to have numbers, otherwise they should be just strings
            final CharSequence message;
            if (userNames.size() == 1) {
                message = res.getString(R.string.dashboard_notifications_activity_message_follow_one,
                        userNames.get(0));
            } else if (userNames.size() == 2) {
                message = res.getString(R.string.dashboard_notifications_activity_message_follow_two,
                        userNames.get(0),
                        userNames.get(1));
            } else if (userNames.size() == 3) {
                message = res.getString(R.string.dashboard_notifications_activity_message_follow_three,
                        userNames.get(0),
                        userNames.get(1));
            } else {
                message = res.getString(R.string.dashboard_notifications_activity_message_follow_many,
                        userNames.get(0),
                        userNames.get(1),
                        userNames.size() - 2);
            }
            return new NotificationMessage(title, message, ticker);
        }

    }
}
