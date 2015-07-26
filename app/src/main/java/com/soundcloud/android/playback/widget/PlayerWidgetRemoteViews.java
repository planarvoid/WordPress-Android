package com.soundcloud.android.playback.widget;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.external.PlaybackAction;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.playback.views.PlaybackRemoteViews;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.utils.ScTextUtils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.preference.PreferenceManager;

/**
 * Play control in the widget.
 *
 * @see com.soundcloud.android.playback.service.PlayerAppWidgetProvider
 */
public class PlayerWidgetRemoteViews extends PlaybackRemoteViews {

    private static final int PENDING_INTENT_REQUEST_CODE = PlayerWidgetRemoteViews.class.hashCode();

    public PlayerWidgetRemoteViews(Context context) {
        super(context.getPackageName(), R.layout.appwidget_player,
                R.drawable.ic_play_arrow_black_36dp, R.drawable.ic_pause_black_36dp);
    }

    /* package */  void setEmptyState(Context context) {
        setPlaybackStatus(false);
        setCurrentTrackTitle(context.getString(R.string.widget_touch_to_open));
        setCurrentCreator(ScTextUtils.EMPTY_STRING);
        linkButtonsWidget(context, Urn.NOT_SET, Urn.NOT_SET, false);
    }

    @SuppressWarnings("UnusedDeclaration")
    public PlayerWidgetRemoteViews(Parcel parcel) {
        super(parcel);
    }

    /* package */ void linkButtonsWidget(Context context, Urn trackUrn, Urn userUrn, boolean addLike) {
        linkPlayerControls(context);

        setOnClickPendingIntent(R.id.title_txt, PendingIntent.getActivity(context,
                PENDING_INTENT_REQUEST_CODE, createLaunchIntent(context, trackUrn), PendingIntent.FLAG_CANCEL_CURRENT));
        if (!trackUrn.equals(Urn.NOT_SET)) {

            Navigator navigator = new Navigator(new FeatureFlags(PreferenceManager.getDefaultSharedPreferences(context))); // Can't inject here :(
            setOnClickPendingIntent(R.id.user_txt, navigator.openProfileFromWidget(context, userUrn, PENDING_INTENT_REQUEST_CODE));

            final Intent toggleLike = new Intent(PlayerWidgetController.ACTION_LIKE_CHANGED);
            toggleLike.putExtra(PlayerWidgetController.EXTRA_ADD_LIKE, addLike);
            setOnClickPendingIntent(R.id.btn_like, PendingIntent.getBroadcast(context,
                    PENDING_INTENT_REQUEST_CODE, toggleLike, PendingIntent.FLAG_CANCEL_CURRENT));
        }
    }

    private Intent createLaunchIntent(Context context, Urn trackUrn) {
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, !trackUrn.equals(Urn.NOT_SET));

        Screen.WIDGET.addToIntent(intent);
        Referrer.PLAYBACK_WIDGET.addToIntent(intent);

        return intent;
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
                .putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, PlayControlEvent.SOURCE_WIDGET)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
    }
}
