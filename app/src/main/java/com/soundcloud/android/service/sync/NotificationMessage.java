package com.soundcloud.android.service.sync;

import static com.soundcloud.android.imageloader.ImageLoader.Options;

import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ImageUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;

import java.util.List;

class NotificationMessage {
    public final CharSequence title, message, ticker;

    public NotificationMessage(Resources res, Activities activities,
                               Activities likes,
                               Activities comments,
                               Activities reposts) {


        if (!reposts.isEmpty() && likes.isEmpty() && comments.isEmpty()) {
            // only reposts
            List<Track> tracks = reposts.getUniqueTracks();
            ticker = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_ticker_repost,
                    reposts.size(),
                    reposts.size());

            title = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_title_repost,
                    reposts.size(),
                    reposts.size());

            if (tracks.size() == 1 && reposts.size() == 1) {
                message = res.getString(R.string.dashboard_notifications_activity_message_repost,
                        reposts.get(0).getUser().username,
                        reposts.get(0).getTrack().title);
            } else {
                message = res.getQuantityString(R.plurals.dashboard_notifications_activity_message_repost,
                        tracks.size(),
                        tracks.get(0).title,
                        (tracks.size() > 1 ? tracks.get(1).title : null));


            }
        } else if (!likes.isEmpty() && comments.isEmpty() && reposts.isEmpty()) {
            // only likes
            List<Track> tracks = likes.getUniqueTracks();
            ticker = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_ticker_like,
                    likes.size(),
                    likes.size());

            title = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_title_like,
                    likes.size(),
                    likes.size());

            if (tracks.size() == 1 && likes.size() == 1) {
                message = res.getString(R.string.dashboard_notifications_activity_message_likes,
                        likes.get(0).getUser().username,
                        likes.get(0).getTrack().title);
            } else {
                message = res.getQuantityString(R.plurals.dashboard_notifications_activity_message_like,
                        tracks.size(),
                        tracks.get(0).title,
                        (tracks.size() > 1 ? tracks.get(1).title : null));
            }
        } else if (!comments.isEmpty() && likes.isEmpty() && reposts.isEmpty()) {
            // only comments
            List<Track> tracks = comments.getUniqueTracks();
            List<User> users = comments.getUniqueUsers();

            ticker = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_ticker_comment,
                    comments.size(),
                    comments.size());

            title = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_title_comment,
                    comments.size(),
                    comments.size());

            if (tracks.size() == 1) {
                message = res.getQuantityString(
                        R.plurals.dashboard_notifications_activity_message_comment_single_track,
                        comments.size(),
                        comments.size(),
                        tracks.get(0).title,
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
            List<Track> tracks = activities.getUniqueTracks();
            List<User> users = activities.getUniqueUsers();
            ticker = res.getQuantityString(R.plurals.dashboard_notifications_activity_ticker_activity,
                    activities.size(),
                    activities.size());

            title = res.getQuantityString(R.plurals.dashboard_notifications_activity_title_activity,
                    activities.size(),
                    activities.size());

            message = res.getQuantityString(R.plurals.dashboard_notifications_activity_message_activity,
                    users.size(),
                    tracks.get(0).title,
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

        if (!Consts.SdkSwitches.useRichNotifications || !ImageUtils.checkIconShouldLoad(artworkUri)) {
            showDashboardNotification(context, ticker, intent, title, message, id, null);
        } else {

            final Bitmap bmp = ImageLoader.get(context).getBitmap(artworkUri, null, Options.dontLoadRemote());
            if (bmp != null){
                showDashboardNotification(context, ticker, intent, title, message, id, bmp);
            } else {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        ImageLoader.get(context).getBitmap(artworkUri, new ImageLoader.BitmapCallback() {
                            public void onImageLoaded(Bitmap loadedBmp, String uri) {
                                showDashboardNotification(context, ticker, intent, title, message, id, loadedBmp);
                            }

                            public void onImageError(String uri, Throwable error) {
                                showDashboardNotification(context, ticker, intent, title, message, id, null);
                            }
                        });
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

        final NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);


        final PendingIntent pi = PendingIntent.getActivity(context.getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        final Notification n = new Notification(R.drawable.ic_notification_cloud, ticker, System.currentTimeMillis());
        n.contentIntent = pi;
        n.flags = Notification.FLAG_AUTO_CANCEL;

        if (bmp == null){
            n.setLatestEventInfo(context.getApplicationContext(), title, message, pi);
        } else {
            final RemoteViews notificationView = new RemoteViews(context.getPackageName(), R.layout.dashboard_notification_v11);
                        notificationView.setTextViewText(R.id.title_txt, title);
                        notificationView.setTextViewText(R.id.content_txt, message);

            notificationView.setImageViewBitmap(R.id.icon,bmp);
            n.contentView = notificationView;
        }
        nm.notify(id, n);
    }

    /* package */ static Intent createNotificationIntent(String action){
        return new Intent(action)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    /* package */ static String getIncomingNotificationMessage(SoundCloudApplication app, Activities activites) {
        List<User> users = activites.getUniqueUsers();
        assert !users.isEmpty();

        switch (users.size()) {
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
