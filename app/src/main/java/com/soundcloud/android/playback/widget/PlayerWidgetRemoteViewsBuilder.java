package com.soundcloud.android.playback.widget;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.graphics.Bitmap;

public class PlayerWidgetRemoteViewsBuilder {

    private Optional<WidgetTrack> optionalTrack;
    private Optional<Boolean> optionalIsPlaying;
    private Optional<Bitmap> optionalArtwork;

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

        if (optionalArtwork != null){
            if (optionalArtwork.isPresent()){
                widgetRemoteView.setImageViewBitmap(R.id.icon, optionalArtwork.get());
            } else {
                widgetRemoteView.setImageViewResource(R.id.icon, R.drawable.appwidget_artwork_placeholder);
            }
        }

        return widgetRemoteView;
    }

    private void setPlayableProperties(Context context, PlayerWidgetRemoteViews widgetRemoteView) {
        WidgetTrack track = optionalTrack.get();

        widgetRemoteView.setImageViewResource(R.id.btn_like, track.isUserLike()
                ? R.drawable.widget_like_orange : R.drawable.widget_like_grey);

        widgetRemoteView.setCurrentTrackTitle(track.getTitle());
        widgetRemoteView.linkButtonsWidget(context, track.getUrn(), track.getUserUrn(), !track.isUserLike());

        widgetRemoteView.setCurrentCreator(track.isAudioAd() ? ScTextUtils.EMPTY_STRING : track.getUserName());
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

    public PlayerWidgetRemoteViewsBuilder forArtwork(Bitmap artwork) {
        this.optionalArtwork = Optional.fromNullable(artwork);
        return this;
    }

    public PlayerWidgetRemoteViewsBuilder forIsPlaying(WidgetTrack track, boolean isPlaying) {
        this.optionalIsPlaying = Optional.of(isPlaying);
        this.optionalTrack  = Optional.of(track);
        return this;
    }

}
