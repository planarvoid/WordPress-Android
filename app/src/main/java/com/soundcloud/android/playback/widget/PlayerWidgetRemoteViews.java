package com.soundcloud.android.playback.widget;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.playback.external.PlaybackAction;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.views.PlaybackRemoteViews;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.utils.ScTextUtils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;

/**
 * Play control in the widget.
 *
 * @see com.soundcloud.android.playback.service.PlayerAppWidgetProvider
 */
public class PlayerWidgetRemoteViews extends PlaybackRemoteViews {

    private static final int PENDING_INTENT_REQUEST_CODE = PlayerWidgetRemoteViews.class.hashCode();

    public PlayerWidgetRemoteViews(Context context) {
        super(context.getPackageName(), R.layout.appwidget_player,
                R.drawable.ic_play_states, R.drawable.ic_pause_states);
    }

    /* package */  void setEmptyState(Context context) {
        setPlaybackStatus(false);
        setCurrentTrackTitle(context.getString(R.string.widget_touch_to_open));
        setCurrentUsername(ScTextUtils.EMPTY_STRING);
        linkButtonsWidget(context, ScModel.NOT_SET, ScModel.NOT_SET, false);
    }

    @SuppressWarnings("UnusedDeclaration")
    public PlayerWidgetRemoteViews(Parcel parcel) {
        super(parcel);
    }

    /* package */ void linkButtonsWidget(Context context, long trackId, long userId, boolean userLike) {
        linkPlayerControls(context);

        final Intent player = new Intent(Actions.PLAYER).addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        // go to player
        setOnClickPendingIntent(R.id.title_txt, PendingIntent.getActivity(context, PENDING_INTENT_REQUEST_CODE, player, 0));
        if (trackId != -1) {
            final Intent browser = new Intent(context, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_USER_ID, userId);
            // go to user
            setOnClickPendingIntent(R.id.user_txt, PendingIntent.getActivity(context,
                    PENDING_INTENT_REQUEST_CODE, browser, PendingIntent.FLAG_UPDATE_CURRENT));

            final Intent toggleLike = new Intent(PlaybackService.Actions.WIDGET_LIKE_CHANGED);
            toggleLike.putExtra(PlaybackService.BroadcastExtras.IS_LIKE, userLike);

            // toggle like
            setOnClickPendingIntent(R.id.btn_like, PendingIntent.getBroadcast(context,
                    PENDING_INTENT_REQUEST_CODE, toggleLike, PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }

    private void linkPlayerControls(Context context) {
        setOnClickPendingIntent(R.id.toggle_playback, createPendingIntent(context, PlaybackAction.TOGGLE_PLAYBACK));
        setOnClickPendingIntent(R.id.prev, createPendingIntent(context, PlaybackAction.PREVIOUS));
        setOnClickPendingIntent(R.id.next, createPendingIntent(context, PlaybackAction.NEXT));
    }

    private PendingIntent createPendingIntent(Context context, String playbackAction) {
        return PendingIntent.getBroadcast(context, PENDING_INTENT_REQUEST_CODE, createIntent(playbackAction), 0);
    }

    private Intent createIntent(String playbackAction) {
        return new Intent(playbackAction)
                .putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, PlayControlEvent.SOURCE_WIDGET);
    }
}
