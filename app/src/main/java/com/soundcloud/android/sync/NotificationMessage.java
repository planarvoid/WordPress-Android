package com.soundcloud.android.sync;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.activities.Activities;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.service.sync.NotificationImageDownloader;
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

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public NotificationMessage(Resources res, Activities activities,
                               Activities likes,
                               Activities comments,
                               Activities reposts) {


        if (!reposts.isEmpty() && likes.isEmpty() && comments.isEmpty()) {
            // only reposts
            List<Playable> playables = reposts.getUniquePlayables();
            ticker = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_ticker_repost,
                    reposts.size(),
                    reposts.size());

            title = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_title_repost,
                    reposts.size(),
                    reposts.size());

            // Note: Transifex requires plurals to have numbers, otherwise they should be just strings
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
        } else if (!likes.isEmpty() && comments.isEmpty() && reposts.isEmpty()) {
            // only likes
            List<Playable> playables = likes.getUniquePlayables();
            ticker = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_ticker_like,
                    likes.size(),
                    likes.size());

            title = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_title_like,
                    likes.size(),
                    likes.size());

            // Note: Transifex requires plurals to have numbers, otherwise they should be just strings
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
        } else if (!comments.isEmpty() && likes.isEmpty() && reposts.isEmpty()) {
            // only comments
            List<Playable> playables = comments.getUniquePlayables();
            List<PublicApiUser> users = comments.getUniqueUsers();

            ticker = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_ticker_comment,
                    comments.size(),
                    comments.size());

            title = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_title_comment,
                    comments.size(),
                    comments.size());

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
        } else {
            // mix of likes, comments, reposts
            List<Playable> playables = activities.getUniquePlayables();
            List<PublicApiUser> users = activities.getUniqueUsers();
            ticker = res.getQuantityString(R.plurals.dashboard_notifications_activity_ticker_activity,
                    activities.size(),
                    activities.size());

            title = res.getQuantityString(R.plurals.dashboard_notifications_activity_title_activity,
                    activities.size(),
                    activities.size());

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
        builder.setContentTitle(title);
        builder.setContentText(message);
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
