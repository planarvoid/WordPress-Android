package com.soundcloud.android.playback.views;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.PlaybackService;
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
public class WidgetPlaybackRemoteViews extends PlaybackRemoteViews {

    private static final int PENDING_INTENT_REQUEST_CODE = WidgetPlaybackRemoteViews.class.hashCode();

    public WidgetPlaybackRemoteViews(Context context) {
        super(context.getPackageName(), R.layout.appwidget_player,
                R.drawable.ic_play_states, R.drawable.ic_pause_states);
        setEmptyState(context);
    }

    public WidgetPlaybackRemoteViews(Args args) {
        super(args.context.getPackageName(), R.layout.appwidget_player,
                R.drawable.ic_play_states, R.drawable.ic_pause_states);

        if (args.trackId != -1) {
            setPlaybackStatus(args.isPlaying);
            setImageViewResource(R.id.btn_like, args.isLiked
                    ? R.drawable.ic_widget_favorited_states : R.drawable.ic_widget_like_states);

            setCurrentTrackTitle(args.title);
            setCurrentUsername(args.username);
            linkButtonsWidget(args.context, args.trackId, args.userId, args.isLiked);
        } else {
            setEmptyState(args.context);
        }
    }

    private void setEmptyState(Context context) {
        setPlaybackStatus(false);
        setCurrentTrackTitle(context.getString(R.string.widget_touch_to_open));
        setCurrentUsername(ScTextUtils.EMPTY_STRING);
        linkButtonsWidget(context, ScModel.NOT_SET, ScModel.NOT_SET, false);
    }

    @SuppressWarnings("UnusedDeclaration")
    public WidgetPlaybackRemoteViews(Parcel parcel) {
        super(parcel);
    }

    private void linkButtonsWidget(Context context, long trackId, long userId, boolean userLike) {
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
        final Intent previous = new Intent(PlaybackService.Actions.PREVIOUS_ACTION)
                .putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, PlayControlEvent.SOURCE_WIDGET);
        setOnClickPendingIntent(R.id.prev, PendingIntent.getService(context,
                PENDING_INTENT_REQUEST_CODE, previous, 0));

        final Intent pause = new Intent(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION)
                .putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, PlayControlEvent.SOURCE_WIDGET);
        setOnClickPendingIntent(R.id.pause, PendingIntent.getService(context,
                PENDING_INTENT_REQUEST_CODE, pause, 0));

        final Intent next = new Intent(PlaybackService.Actions.NEXT_ACTION)
                .putExtra(PlayControlEvent.EXTRA_EVENT_SOURCE, PlayControlEvent.SOURCE_WIDGET);
        setOnClickPendingIntent(R.id.next, PendingIntent.getService(context,
                PENDING_INTENT_REQUEST_CODE, next, 0));
    }

    public static class Args {
        private final Context context;
        private final long trackId;
        private final long userId;
        private final String title;
        private final String username;
        private final boolean isPlaying;
        private final boolean isLiked;

        public Args(Context context, Playable playable, boolean isPlaying) {
            this.context = context;
            trackId = playable.getId();
            userId = playable.getUserId();
            title = playable.getTitle();
            username = playable.getUsername();
            isLiked = playable.user_like;
            this.isPlaying = isPlaying;
        }

        public Args(Context context, Intent intent) {
            this.context = context;
            trackId = intent.getLongExtra(PlaybackService.BroadcastExtras.ID, ScModel.NOT_SET);
            userId = intent.getLongExtra(PlaybackService.BroadcastExtras.USER_ID, ScModel.NOT_SET);
            title = intent.getStringExtra(PlaybackService.BroadcastExtras.TITLE);
            username = intent.getStringExtra(PlaybackService.BroadcastExtras.USERNAME);
            isLiked = intent.getBooleanExtra(PlaybackService.BroadcastExtras.IS_LIKE, false);
            isPlaying = Playa.StateTransition.fromIntent(intent).playSessionIsActive();
        }
    }
}
