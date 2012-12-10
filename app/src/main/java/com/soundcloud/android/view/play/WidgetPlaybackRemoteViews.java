package com.soundcloud.android.view.play;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.service.playback.CloudPlaybackService;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;

/**
 * Play control in the widget.
 * @see com.soundcloud.android.service.playback.PlayerAppWidgetProvider
 */
public class WidgetPlaybackRemoteViews extends PlaybackRemoteViews {
    public WidgetPlaybackRemoteViews(String packageName) {
        super(packageName, R.layout.appwidget_player,
                R.drawable.ic_play_states,
                R.drawable.ic_pause_states);
    }

    @SuppressWarnings("UnusedDeclaration")
    public WidgetPlaybackRemoteViews(Parcel parcel) {
        super(parcel);
    }

    public void linkButtonsWidget(Context context, long trackId, long userId, boolean userLike) {
        linkPlayerControls(context);

        final Intent player = new Intent(Actions.PLAYER).addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        // go to player
        setOnClickPendingIntent(R.id.title_txt, PendingIntent.getActivity(context, 0, player, 0));
        if (trackId != -1) {
            final Intent browser = new Intent(context, UserBrowser.class).putExtra("userId", userId);
            // go to user
            setOnClickPendingIntent(R.id.user_txt,
                    PendingIntent.getActivity(context, 0, browser, PendingIntent.FLAG_UPDATE_CURRENT));

            final Intent toggleLike = new Intent(
                    userLike ?
                            CloudPlaybackService.REMOVE_LIKE_ACTION :
                            CloudPlaybackService.ADD_LIKE_ACTION)
                    .putExtra(CloudPlaybackService.EXTRA_TRACK_ID, trackId);

            // toggle like
            setOnClickPendingIntent(R.id.btn_like, PendingIntent.getService(context,
                    0 /* requestCode */, toggleLike, PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }
}
