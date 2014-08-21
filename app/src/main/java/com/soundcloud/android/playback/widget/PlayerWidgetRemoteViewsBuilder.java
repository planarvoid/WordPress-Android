package com.soundcloud.android.playback.widget;

import com.google.common.base.Optional;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;

public class PlayerWidgetRemoteViewsBuilder {

    private Optional<WidgetTrack> optionalTrack;
    private Optional<Boolean> optionalIsPlaying;

    public PlayerWidgetRemoteViewsBuilder() {
        optionalTrack = Optional.absent();
        optionalIsPlaying = Optional.absent();
    }

    public PlayerWidgetRemoteViews build(Context context) {
        PlayerWidgetRemoteViews widgetRemoteView = new PlayerWidgetRemoteViews(context);

        if (!optionalIsPlaying.isPresent() && !optionalTrack.isPresent()) {
            setEmptyState(context, widgetRemoteView);
        }

        if (optionalIsPlaying.isPresent()) {
            setPlaybackStatus(widgetRemoteView);
        }

        if (optionalTrack.isPresent()) {
            setPlayableProperties(context, widgetRemoteView);
        }

        return widgetRemoteView;
    }

    private void setPlayableProperties(Context context, PlayerWidgetRemoteViews widgetRemoteView) {
        WidgetTrack track = optionalTrack.get();

        widgetRemoteView.setImageViewResource(R.id.btn_like, track.isUserLike()
                ? R.drawable.ic_widget_favorited_states : R.drawable.ic_widget_like_states);

        widgetRemoteView.setCurrentTrackTitle(track.getTitle());
        widgetRemoteView.linkButtonsWidget(context, track.getUrn(), track.getUserUrn(), track.isUserLike());

        widgetRemoteView.setCurrentUsername(track.isAudioAd() ? ScTextUtils.EMPTY_STRING : track.getUserName());
        widgetRemoteView.setLikeShown(!track.isAudioAd());
    }

    private void setPlaybackStatus(PlayerWidgetRemoteViews widgetRemoteView) {
        widgetRemoteView.setPlaybackStatus(optionalIsPlaying.get());
    }

    private void setEmptyState(Context context, PlayerWidgetRemoteViews widgetRemoteView) {
        widgetRemoteView.setEmptyState(context);
    }

    public PlayerWidgetRemoteViewsBuilder forTrack(WidgetTrack widgetTrack) {
        this.optionalTrack = Optional.of(widgetTrack);
        return this;
    }

    public PlayerWidgetRemoteViewsBuilder forIsPlaying(boolean isPlaying) {
        this.optionalIsPlaying = Optional.of(isPlaying);
        return this;
    }

}
