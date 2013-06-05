package com.soundcloud.android.service.sync;

import static com.soundcloud.android.imageloader.ImageLoader.Options;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.utils.images.ImageSize;
import com.soundcloud.android.utils.images.ImageUtils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;

import java.util.List;

class NotificationMessage {
    public final CharSequence title, message, ticker;

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

            if (playables.size() == 1 && reposts.size() == 1) {
                message = res.getString(R.string.dashboard_notifications_activity_message_repost,
                        reposts.get(0).getUser().username,
                        reposts.get(0).getPlayable().title);
            } else {
                message = res.getQuantityString(R.plurals.dashboard_notifications_activity_message_repost,
                        playables.size(),
                        playables.get(0).title,
                        (playables.size() > 1 ? playables.get(1).title : null));


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

            if (playables.size() == 1 && likes.size() == 1) {
                message = res.getString(R.string.dashboard_notifications_activity_message_likes,
                        likes.get(0).getUser().username,
                        likes.get(0).getPlayable().title);
            } else {
                message = res.getQuantityString(R.plurals.dashboard_notifications_activity_message_like,
                        playables.size(),
                        playables.get(0).title,
                        (playables.size() > 1 ? playables.get(1).title : null));
            }
        } else if (!comments.isEmpty() && likes.isEmpty() && reposts.isEmpty()) {
            // only comments
            List<Playable> playables = comments.getUniquePlayables();
            List<User> users = comments.getUniqueUsers();

            ticker = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_ticker_comment,
                    comments.size(),
                    comments.size());

            title = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_title_comment,
                    comments.size(),
                    comments.size());

            if (playables.size() == 1) {
                message = res.getQuantityString(
                        R.plurals.dashboard_notifications_activity_message_comment_single_track,
                        comments.size(),
                        comments.size(),
                        playables.get(0).title,
                        comments.get(0).getUser().username,
                        comments.size() > 1 ? comments.get(1).getUser().username : null);
            } else {
                message = res.getQuantityString(R.plurals.dashboard_notifications_activity_message_comment,
                                users.size(),
                                users.get(0).username,
                                (users.size() > 1 ? users.get(1).username : null));
            }
        } else {
           // mix of likes, comments, reposts
            List<Playable> playables = activities.getUniquePlayables();
            List<User> users = activities.getUniqueUsers();
            ticker = res.getQuantityString(R.plurals.dashboard_notifications_activity_ticker_activity,
                    activities.size(),
                    activities.size());

            title = res.getQuantityString(R.plurals.dashboard_notifications_activity_title_activity,
                    activities.size(),
                    activities.size());

            message = res.getQuantityString(R.plurals.dashboard_notifications_activity_message_activity,
                    users.size(),
                    playables.get(0).title,
                    users.get(0).username,
                    users.size() > 1 ? users.get(1).username : null);
        }
    }

    static void showNewFollower(SoundCloudApplication app, User u) {
        showDashboardNotification(app,
                app.getString(R.string.dashboard_notifications_ticker_follower),
                app.getString(R.string.dashboard_notifications_title_follower),
                app.getString(R.string.dashboard_notifications_message_follower, u.username),
                createNotificationIntent(Actions.USER_BROWSER).putExtra("user",u),
                Consts.Notifications.DASHBOARD_NOTIFY_STREAM_ID,
                u.avatar_url);
    }

    /* package */  static void showDashboardNotification(final Context context,
                                                  final CharSequence ticker,
                                                  final CharSequence title,
                                                  final CharSequence message,
                                                  final Intent intent,
                                                  final int id,
                                                  final String artworkUri) {

        final String largeIcon = ImageSize.formatUriForNotificationLargeIcon(context, artworkUri);
        if (!Consts.SdkSwitches.useRichNotifications || !ImageUtils.checkIconShouldLoad(largeIcon)) {
            showDashboardNotification(context, ticker, intent, title, message, id, null);
        } else {

            final Bitmap bmp = ImageLoader.get(context).getBitmap(largeIcon, null, null, Options.dontLoadRemote());
            if (bmp != null){
                showDashboardNotification(context, ticker, intent, title, message, id, bmp);
            } else {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        ImageLoader.get(context).getBitmap(largeIcon, new ImageLoader.BitmapLoadCallback() {
                            public void onImageLoaded(Bitmap loadedBmp, String uri) {
                                showDashboardNotification(context, ticker, intent, title, message, id, loadedBmp);
                            }

                            public void onImageError(String uri, Throwable error) {
                                showDashboardNotification(context, ticker, intent, title, message, id, null);
                            }
                        }, context);
                    }
                });
            }
        }
    }

     /* package */ static void showDashboardNotification(Context context,
                                                  CharSequence ticker,
                                                  Intent intent,
                                                  CharSequence title,
                                                  CharSequence message,
                                                  int id,
                                                  Bitmap bmp) {

        final PendingIntent pendingIntent = PendingIntent.getActivity(context.getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

         NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
         builder.setSmallIcon(R.drawable.ic_notification_cloud);
         builder.setContentIntent(pendingIntent);
         builder.setAutoCancel(true);
         builder.setTicker(ticker);
         builder.setContentTitle(title);
         builder.setContentText(message);
         if (bmp != null) builder.setLargeIcon(bmp);
         ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, builder.build());
    }

    /* package */ static Intent createNotificationIntent(String action){
        return new Intent(action)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    /* package */ static String getIncomingNotificationMessage(SoundCloudApplication app, Activities activites) {
        List<User> users = activites.getUniqueUsers();
        switch (users.size()) {
            case 0:
                return ""; // should not get this far, but in case

            case 1:
                return String.format(
                        app.getString(R.string.dashboard_notifications_message_incoming),
                        users.get(0).username);
            case 2:
                return String.format(
                        app.getString(R.string.dashboard_notifications_message_incoming_2),
                        users.get(0).username, users.get(1).username);
            default:
                return String.format(
                        app.getString(R.string.dashboard_notifications_message_incoming_others),
                        users.get(0).username, users.get(1).username);

        }
    }
}
