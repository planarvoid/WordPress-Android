package com.soundcloud.android.playback.views;

import com.soundcloud.android.R;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.playback.external.PlaybackAction;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.view.View;

import javax.inject.Inject;

/**
 * Play controls used in notifications (API level 11 and up)
 */
public class NotificationPlaybackRemoteViews extends PlaybackRemoteViews {

    private static final int PENDING_INTENT_REQUEST_CODE = NotificationPlaybackRemoteViews.class.hashCode();

    NotificationPlaybackRemoteViews(String packageName, int layout) {
        super(packageName,
                layout,
                R.drawable.notifications_play,
                R.drawable.notifications_pause);
    }

    @SuppressWarnings("UnusedDeclaration")
    public NotificationPlaybackRemoteViews(Parcel parcel) {
        super(parcel);
    }

    public void linkButtonsNotification(Context context) {
        linkPlayerControls(context);
        setOnClickPendingIntent(R.id.close, createPendingIntent(context, PlaybackAction.CLOSE));
    }

    public void setIcon(Bitmap bitmap) {
        if (bitmap != null){
            setImageViewBitmap(R.id.icon, bitmap);
            setViewVisibility(R.id.icon, View.VISIBLE);
        } else {
            setViewVisibility(R.id.icon,View.GONE);
        }
    }

    public void clearIcon() {
        setViewVisibility(R.id.icon,View.GONE);
    }

    private void linkPlayerControls(Context context) {
        setOnClickPendingIntent(R.id.toggle_playback, createPendingIntent(context, PlaybackAction.TOGGLE_PLAYBACK));
        setOnClickPendingIntent(R.id.next, createPendingIntent(context, PlaybackAction.NEXT));
        setOnClickPendingIntent(R.id.prev, createPendingIntent(context, PlaybackAction.PREVIOUS));
    }

    public void setCurrentCreator(CharSequence creator) {
        setTextViewText(R.id.user_txt, creator);
    }

    private PendingIntent createPendingIntent(Context context, String playbackAction) {
        return PendingIntent.getBroadcast(context, PENDING_INTENT_REQUEST_CODE, createIntent(playbackAction), 0);
    }

    private Intent createIntent(String playbackAction) {
        return new Intent(playbackAction)
                .putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, PlayControlEvent.SOURCE_NOTIFICATION);
    }

    public static class Factory {
        @Inject
        public Factory() {
        }

        public NotificationPlaybackRemoteViews create(String packageName) {
            return create(packageName, R.layout.playback_status);
        }

        public NotificationPlaybackRemoteViews create(String packageName, int layout){
            return new NotificationPlaybackRemoteViews(packageName, layout);
        }
    }
}
