package com.soundcloud.android.sync;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.activities.Activities;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.service.sync.NotificationImageDownloader;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.images.ImageUtils;
import org.jetbrains.annotations.Nullable;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;

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
        private Activities likes = Activities.EMPTY;
        private Activities comments = Activities.EMPTY;
        private Activities reposts = Activities.EMPTY;
        private Activities followers = Activities.EMPTY;
        private Activities allActivitiesToNotify = Activities.EMPTY;

        Builder(Resources resources) {
            this.res = resources;
        }

        public Builder setLikes(Activities likes) {
            this.likes = likes;
            return this;
        }

        public Builder setComments(Activities comments) {
            this.comments = comments;
            return this;
        }

        public Builder setReposts(Activities reposts) {
            this.reposts = reposts;
            return this;
        }

        public Builder setFollowers(Activities followers) {
            this.followers = followers;
            return this;
        }

        // This setter is actually all activities to notify (likes, comments, ...)
        // In the next refactor step we could get rid of it and build this in
        // `buildMixedActivitiesNotification`. Your friend.
        public Builder setAllActivitiesToNotify(Activities allActivitiesToNotify) {
            this.allActivitiesToNotify = allActivitiesToNotify;
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

        private NotificationMessage buildMixedActivitiesNotification() {
            List<Playable> playables = allActivitiesToNotify.getUniquePlayables();
            List<PublicApiUser> users = allActivitiesToNotify.getUniqueUsers();
            final CharSequence ticker = res.getQuantityString(R.plurals.dashboard_notifications_activity_ticker_activity,
                    allActivitiesToNotify.size(),
                    allActivitiesToNotify.size());

            final CharSequence title = res.getQuantityString(R.plurals.dashboard_notifications_activity_title_activity,
                    allActivitiesToNotify.size(),
                    allActivitiesToNotify.size());

            final CharSequence message;
            if (users.size() == 1) {
                if (playables.size() == 1) {
                    message = res.getString(R.string.dashboard_notifications_activity_message_activity_one_user_one_playable,
                            users.get(0).username,
                            playables.get(0).title);
                } else {
                    message = res.getString(R.string.dashboard_notifications_activity_message_activity_one_user_multiple_playables,
                            users.get(0).username);
                }
            } else if (users.size() == 2) {
                message = res.getString(R.string.dashboard_notifications_activity_message_activity_two,
                        users.get(0).username,
                        users.get(1).username);
            } else {
                message = res.getString(R.string.dashboard_notifications_activity_message_activity_other,
                        users.get(0).username,
                        users.get(1).username);
            }
            return new NotificationMessage(title, message, ticker);
        }

        private NotificationMessage buildCommentsOnlyNotification() {
            List<Playable> playables = comments.getUniquePlayables();
            List<PublicApiUser> users = comments.getUniqueUsers();

            final CharSequence ticker = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_ticker_comment,
                    comments.size(),
                    comments.size());

            final CharSequence title = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_title_comment,
                    comments.size(),
                    comments.size());

            final CharSequence message;
            if (playables.size() == 1) {
                if (comments.size() == 1) {
                    message = res.getString(
                            R.string.dashboard_notifications_activity_message_comment_single_track_one,
                            playables.get(0).title,
                            comments.get(0).getUser().username);
                } else if (comments.size() == 2) {
                    message = res.getString(
                            R.string.dashboard_notifications_activity_message_comment_single_track_two,
                            comments.size(),
                            playables.get(0).title,
                            comments.get(0).getUser().username,
                            comments.get(1).getUser().username);
                } else {
                    message = res.getString(
                            R.string.dashboard_notifications_activity_message_comment_single_track_other,
                            comments.size(),
                            playables.get(0).title,
                            comments.get(0).getUser().username,
                            comments.get(1).getUser().username);
                }
            } else {
                if (users.size() == 1) {
                    message = res.getString(R.string.dashboard_notifications_activity_message_comment_one,
                            users.get(0).username);
                } else if (users.size() == 2) {
                    message = res.getString(R.string.dashboard_notifications_activity_message_comment_two,
                            users.get(0).username,
                            users.get(1).username);
                } else {
                    message = res.getString(R.string.dashboard_notifications_activity_message_comment_other,
                            users.get(0).username,
                            users.get(1).username);
                }
            }
            return new NotificationMessage(title, message, ticker);
        }

        private NotificationMessage buildLikesOnlyNotification() {
            List<Playable> playables = likes.getUniquePlayables();
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
            if (playables.size() == 1 && likes.size() == 1) {
                message = res.getString(R.string.dashboard_notifications_activity_message_likes,
                        likes.get(0).getUser().username,
                        likes.get(0).getPlayable().title);
            } else if (playables.size() == 1) {
                message = res.getString(R.string.dashboard_notifications_activity_message_like_one,
                        playables.get(0).title);
            } else if (playables.size() == 2) {
                message = res.getString(R.string.dashboard_notifications_activity_message_like_two,
                        playables.get(0).title,
                        playables.get(1).title);
            } else {
                message = res.getString(R.string.dashboard_notifications_activity_message_like_other,
                        playables.get(0).title,
                        playables.get(1).title);
            }
            return new NotificationMessage(title, message, ticker);
        }

        private NotificationMessage buildRepostsOnlyNotification() {
            List<Playable> playables = reposts.getUniquePlayables();
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
            if (playables.size() == 1 && reposts.size() == 1) {
                message = res.getString(R.string.dashboard_notifications_activity_message_repost,
                        reposts.get(0).getUser().username,
                        reposts.get(0).getPlayable().title);
            } else if (playables.size() == 1) {
                message = res.getString(R.string.dashboard_notifications_activity_message_repost_one,
                        playables.get(0).title);
            } else if (playables.size() == 2) {
                message = res.getString(R.string.dashboard_notifications_activity_message_repost_two,
                        playables.get(0).title,
                        playables.get(1).title);
            } else {
                message = res.getString(R.string.dashboard_notifications_activity_message_repost_other,
                        playables.get(0).title,
                        playables.get(1).title);
            }
            return new NotificationMessage(title, message, ticker);
        }

        private NotificationMessage buildFollowersOnlyNotification() {
            List<PublicApiUser> users = followers.getUniqueUsers();
            final CharSequence ticker = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_follower,
                    users.size(),
                    users.size());

            final CharSequence title = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_follower,
                    users.size(),
                    users.size());

            // Note: Transifex requires plurals to have numbers, otherwise they should be just strings
            final CharSequence message;
            if (users.size() == 1) {
                message = res.getString(R.string.dashboard_notifications_activity_message_follow_one,
                        users.get(0).getDisplayName());
            } else if (users.size() == 2) {
                message = res.getString(R.string.dashboard_notifications_activity_message_follow_two,
                        users.get(0).getDisplayName(),
                        users.get(1).getDisplayName());
            } else if (users.size() == 3) {
                message = res.getString(R.string.dashboard_notifications_activity_message_follow_three,
                        users.get(0).getDisplayName(),
                        users.get(1).getDisplayName());
            } else {
                message = res.getString(R.string.dashboard_notifications_activity_message_follow_many,
                        users.get(0).getDisplayName(),
                        users.get(1).getDisplayName(),
                        users.size() - 2);
            }
            return new NotificationMessage(title, message, ticker);
        }

    }

    /* package */
    static void showDashboardNotification(final Context context,
                                          final CharSequence ticker,
                                          final CharSequence title,
                                          final CharSequence message,
                                          final Intent intent,
                                          final int id,
                                          final String artworkUri) {
        final String largeIconUri = ApiImageSize.formatUriForNotificationLargeIcon(context, artworkUri);
        if (!ImageUtils.checkIconShouldLoad(largeIconUri)) {
            showDashboardNotification(context, ticker, intent, title, message, id, null);
        } else {
            // cannot use imageloader here as the weak reference to the fake image will get dropped and the image won't load (sometimes)
            new NotificationImageDownloader() {
                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    showDashboardNotification(context, ticker, intent, title, message, id, bitmap);
                }
            }.execute(largeIconUri);
        }
    }

    /* package */
    static void showDashboardNotification(Context context,
                                          CharSequence ticker,
                                          Intent intent,
                                          CharSequence title,
                                          CharSequence message,
                                          int id,
                                          @Nullable Bitmap bmp) {

        final PendingIntent pendingIntent = PendingIntent.getActivity(context.getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notification_cloud);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setTicker(ticker);
        builder.setContentTitle(context.getResources().getString(R.string.app_name));
        builder.setContentText(title + ScTextUtils.SPACE_SEPARATOR + message);

        if (bmp != null) {
            builder.setLargeIcon(bmp);
        }
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, builder.build());
    }

    /* package */
    static Intent createNotificationIntent(String action) {
        Intent intent = new Intent(action)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Screen.NOTIFICATION.addToIntent(intent);
        Referrer.ACTIVITIES_NOTIFICATION.addToIntent(intent);
        return intent;
    }
}
