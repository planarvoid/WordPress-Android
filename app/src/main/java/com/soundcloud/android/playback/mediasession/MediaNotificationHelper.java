package com.soundcloud.android.playback.mediasession;

import static android.view.KeyEvent.KEYCODE_MEDIA_NEXT;
import static android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
import static android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS;
import static android.view.KeyEvent.KEYCODE_MEDIA_STOP;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.java.optional.Optional;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.view.KeyEvent;

class MediaNotificationHelper {
    public static final int NOTIFICATION_ICON = R.drawable.ic_notification_cloud;
    public static final int PREVIOUS_ACTION_POSITION = 0;
    public static final int PLAY_PAUSE_ACTION_POSITION = 1;
    public static final int NEXT_ACTION_POSITION = 2;

    static Optional<NotificationCompat.Builder> from(Context context,
                                                     MediaSessionCompat mediaSession,
                                                     boolean playing) {
        Optional<MediaDescriptionCompat> descriptionOpt = getMediaDescription(mediaSession);

        if (descriptionOpt.isPresent()) {
            MediaDescriptionCompat description = descriptionOpt.get();

            return Optional.of((NotificationCompat.Builder) new NotificationCompat
                    .Builder(context)
                    .setContentTitle(description.getTitle())
                    .setContentText(description.getSubtitle())
                    .setSubText(description.getDescription())
                    .setSmallIcon(NOTIFICATION_ICON)
                    .setLargeIcon(description.getIconBitmap())
                    .setContentIntent(getContentIntent(context))
                    .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                    .setShowWhen(false)
                    .setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setDeleteIntent(getActionIntent(context, KEYCODE_MEDIA_STOP))
                    .addAction(createAction(context, Action.PREVIOUS))
                    .addAction(createAction(context, playing ? Action.PAUSE : Action.PLAY))
                    .addAction(createAction(context, Action.NEXT))
                    .setStyle(createMediaStyle(context, mediaSession)));
        } else {
            return Optional.absent();
        }
    }

    private static Optional<MediaDescriptionCompat> getMediaDescription(MediaSessionCompat mediaSession) {
        MediaControllerCompat controller = mediaSession.getController();

        if (controller != null) {
            MediaMetadataCompat metadata = controller.getMetadata();

            if (metadata != null) {
                return Optional.of(metadata.getDescription());
            }
        }

        return Optional.absent();
    }

    private static NotificationCompat.MediaStyle createMediaStyle(Context context, MediaSessionCompat mediaSession) {
        return new NotificationCompat.MediaStyle()
                .setShowCancelButton(true)
                .setCancelButtonIntent(getActionIntent(context, KEYCODE_MEDIA_STOP))
                .setShowActionsInCompactView(
                        PREVIOUS_ACTION_POSITION, PLAY_PAUSE_ACTION_POSITION, NEXT_ACTION_POSITION)
                .setMediaSession(mediaSession.getSessionToken());
    }

    private static NotificationCompat.Action createAction(Context context, Action action) {
        return new NotificationCompat.Action(
                action.drawable, context.getString(action.text),
                MediaNotificationHelper.getActionIntent(context, action.keyCode));
    }

    private static PendingIntent getActionIntent(Context context, int keyCode) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setPackage(context.getPackageName());
        intent.putExtra(Intent.EXTRA_KEY_EVENT,
                        new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        return PendingIntent.getBroadcast(context, keyCode, intent, 0);
    }

    private static PendingIntent getContentIntent(Context context) {
        final Intent intent = new Intent(context, LauncherActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true);

        Screen.NOTIFICATION.addToIntent(intent);
        Referrer.PLAYBACK_NOTIFICATION.addToIntent(intent);

        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private enum Action {
        PREVIOUS(KEYCODE_MEDIA_PREVIOUS, R.drawable.notifications_previous, R.string.previous),
        PAUSE(KEYCODE_MEDIA_PLAY_PAUSE, R.drawable.notifications_pause, R.string.pause),
        PLAY(KEYCODE_MEDIA_PLAY_PAUSE, R.drawable.notifications_play, R.string.play),
        NEXT(KEYCODE_MEDIA_NEXT, R.drawable.notifications_next, R.string.next);

        final int keyCode;
        final int drawable;
        final int text;

        Action(int keyCode, int drawable, int text) {
            this.keyCode = keyCode;
            this.drawable = drawable;
            this.text = text;
        }
    }
}
