package com.soundcloud.android.service.sync;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Activities;
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
import android.widget.RemoteViews;

import java.util.List;

class Message {
    public final CharSequence title, message, ticker;
    public Message(Resources res, Activities activities, Activities favoritings, Activities comments) {
        if (!favoritings.isEmpty() && comments.isEmpty()) {
            // only favoritings
            List<Track> tracks = favoritings.getUniqueTracks();
            ticker = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_ticker_like,
                    favoritings.size(),
                    favoritings.size());

            title = res.getQuantityString(
                    R.plurals.dashboard_notifications_activity_title_like,
                    favoritings.size(),
                    favoritings.size());

            if (tracks.size() == 1 && favoritings.size() == 1) {
                message = res.getString(R.string.dashboard_notifications_activity_message_likes,
                        favoritings.get(0).getUser().username,
                        favoritings.get(0).getTrack().title);
            } else {
                message = res.getQuantityString(R.plurals.dashboard_notifications_activity_message_like,
                        tracks.size(),
                        tracks.get(0).title,
                        (tracks.size() > 1 ? tracks.get(1).title : null));
            }
        } else if (favoritings.isEmpty() && !comments.isEmpty()) {
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
           // mix of favoritings and comments
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
                                                  String artworkUri) {

        if (!SoundCloudApplication.useRichNotifications() || !ImageUtils.checkIconShouldLoad(artworkUri)) {
            showDashboardNotification(context, ticker, intent, title, message, id, null);
        } else {
            final Bitmap bmp = ImageLoader.get(context).getBitmap(artworkUri,null, new ImageLoader.Options(false));
            if (bmp != null){
                showDashboardNotification(context, ticker, intent, title, message, id, bmp);
            } else {
                ImageLoader.get(context).getBitmap(artworkUri,new ImageLoader.BitmapCallback(){
                    public void onImageLoaded(Bitmap loadedBmp, String uri) {
                        showDashboardNotification(context, ticker, intent, title, message, id, loadedBmp);
                    }
                    public void onImageError(String uri, Throwable error) {
                        showDashboardNotification(context, ticker, intent, title, message, id, null);
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

        final Notification n = new Notification(R.drawable.ic_status, ticker, System.currentTimeMillis());
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

    /* package */ static String getExclusiveNotificationMessage(SoundCloudApplication app, Activities activities) {
        if (activities.size() == 1) {
            return String.format(
                    app.getString(R.string.dashboard_notifications_message_single_exclusive),
                    activities.get(0).getTrack().user.username);

        } else {
            List<User> users = activities.getUniqueUsers();
            assert !users.isEmpty();

            switch (users.size()) {
                case 1:
                    return String.format(
                            app.getString(R.string.dashboard_notifications_message_exclusive),
                            users.get(0).username);
                case 2:
                    return String.format(
                            app.getString(R.string.dashboard_notifications_message_exclusive_2),
                            users.get(0).username, users.get(1).username);
                default:
                    return String.format(app
                            .getString(R.string.dashboard_notifications_message_exclusive_others),
                            users.get(0).username, users.get(1).username);
            }
        }
    }
}
