package com.soundcloud.android.playback.widget;

import com.google.common.base.Optional;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.Playable;

import android.content.Context;

public class PlayerWidgetRemoteViewsBuilder {

    private Optional<Playable> optionalPlayable;
    private Optional<Boolean> optionalIsPlaying;

    public PlayerWidgetRemoteViewsBuilder() {
        optionalPlayable = Optional.absent();
        optionalIsPlaying = Optional.absent();
    }

    public PlayerWidgetRemoteViews build(Context context) {
        PlayerWidgetRemoteViews widgetRemoteView = new PlayerWidgetRemoteViews(context);

        if (!optionalIsPlaying.isPresent() && !optionalPlayable.isPresent()) {
            setEmptyState(context, widgetRemoteView);
        }

        if (optionalIsPlaying.isPresent()) {
            setPlaybackStatus(widgetRemoteView);
        }

        if (optionalPlayable.isPresent()) {
            setPlayableProperties(context, widgetRemoteView);
        }

        return widgetRemoteView;
    }

    private void setPlayableProperties(Context context, PlayerWidgetRemoteViews widgetRemoteView) {
        Playable playable = optionalPlayable.get();

        widgetRemoteView.setImageViewResource(R.id.btn_like, playable.user_like
                ? R.drawable.ic_widget_favorited_states : R.drawable.ic_widget_like_states);

        widgetRemoteView.setCurrentTrackTitle(playable.getTitle());
        widgetRemoteView.setCurrentUsername(playable.getUsername());
        widgetRemoteView.linkButtonsWidget(context, playable.getId(), playable.getUserId(), playable.user_like);
    }

    private void setPlaybackStatus(PlayerWidgetRemoteViews widgetRemoteView) {
        widgetRemoteView.setPlaybackStatus(optionalIsPlaying.get());
    }

    private void setEmptyState(Context context, PlayerWidgetRemoteViews widgetRemoteView) {
        widgetRemoteView.setEmptyState(context);
    }

    public PlayerWidgetRemoteViewsBuilder forPlayable(Playable playable) {
        this.optionalPlayable = Optional.of(playable);
        return this;
    }

    public PlayerWidgetRemoteViewsBuilder forIsPlaying(boolean isPlaying) {
        this.optionalIsPlaying = Optional.of(isPlaying);
        return this;
    }

}
