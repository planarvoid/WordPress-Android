package com.soundcloud.android.playback.widget;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.external.PlaybackAction;
import com.soundcloud.android.playback.external.PlaybackActionReceiver;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.playback.views.PlaybackRemoteViews;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

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

    public static final Creator<PlayerWidgetRemoteViews> CREATOR = new Creator<PlayerWidgetRemoteViews>() {
        @Override
        public PlayerWidgetRemoteViews createFromParcel(Parcel source) {
            return new PlayerWidgetRemoteViews(source);
        }

        @Override
        public PlayerWidgetRemoteViews[] newArray(int size) {
            return new PlayerWidgetRemoteViews[size];
        }
    };

    private static final int PENDING_INTENT_REQUEST_CODE = PlayerWidgetRemoteViews.class.hashCode();

    public PlayerWidgetRemoteViews(Context context) {
        super(context.getPackageName(), R.layout.appwidget_player,
              R.drawable.ic_play_arrow_black_36dp, R.drawable.ic_pause_black_36dp);
    }

    void setEmptyState(Context context) {
        setPlaybackStatus(false);
        setCurrentTrackTitle(context.getString(R.string.widget_touch_to_open));
        setCurrentCreator(Strings.EMPTY);
        linkPlayControls(context, false);
    }

    PlayerWidgetRemoteViews(Parcel parcel) {
        super(parcel);
    }

    void linkPlayControls(Context context, boolean isPlayableFromWidget) {
        setOnClickPendingIntent(R.id.toggle_playback, isPlayableFromWidget
                                                      ?
                                                      createPlaybackPendingIntent(context,
                                                                                  PlaybackAction.TOGGLE_PLAYBACK)
                                                      :
                                                      createLaunchPendingIntent(context));
        setOnClickPendingIntent(R.id.prev, createPlaybackPendingIntent(context, PlaybackAction.PREVIOUS));
        setOnClickPendingIntent(R.id.next, createPlaybackPendingIntent(context, PlaybackAction.NEXT));
    }

    private PendingIntent createLaunchPendingIntent(Context context) {
        return PendingIntent.getActivity(context,
                                         R.id.player_widget_request_id,
                                         new Intent(context,
                                                    MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                                         PendingIntent.FLAG_CANCEL_CURRENT);
    }

    void linkTitles(Context context, Urn trackUrn, Optional<Urn> userUrn) {
        setOnClickPendingIntent(R.id.title_txt, PendingIntent.getActivity(context,
                                                                          PENDING_INTENT_REQUEST_CODE,
                                                                          createLaunchIntent(context, trackUrn),
                                                                          PendingIntent.FLAG_CANCEL_CURRENT));

        if (userUrn.isPresent()) {
            setOnClickPendingIntent(R.id.user_txt,
                                    Navigator.openProfileFromWidget(context,
                                                                    userUrn.get(),
                                                                    PENDING_INTENT_REQUEST_CODE));
        }
    }

    void linkLikeToggle(Context context, Optional<Boolean> isLiked) {
        if (isLiked.isPresent()) {
            final Intent toggleLike = new Intent(PlayerWidgetController.ACTION_LIKE_CHANGED);
            toggleLike.putExtra(PlayerWidgetController.EXTRA_ADD_LIKE, !isLiked.get());
            setOnClickPendingIntent(R.id.btn_like, PendingIntent.getBroadcast(context,
                                                                              PENDING_INTENT_REQUEST_CODE,
                                                                              toggleLike,
                                                                              PendingIntent.FLAG_CANCEL_CURRENT));
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

    private PendingIntent createPlaybackPendingIntent(Context context, String playbackAction) {
        return PendingIntent.getBroadcast(context, PENDING_INTENT_REQUEST_CODE, createIntent(playbackAction), 0);
    }

    private Intent createIntent(String playbackAction) {
        return new Intent(playbackAction)
                .putExtra(PlaybackActionReceiver.EXTRA_EVENT_SOURCE, PlaybackActionReceiver.SOURCE_WIDGET)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
    }
}
