package com.soundcloud.android.playback.widget;

import com.soundcloud.android.R;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.graphics.Bitmap;

class PlayerWidgetRemoteViewsBuilder {

    private Optional<WidgetItem> optionalItem;
    private Optional<Boolean> optionalIsPlaying;
    private Optional<Bitmap> optionalArtwork;

    PlayerWidgetRemoteViewsBuilder() {
        optionalItem = Optional.absent();
        optionalIsPlaying = Optional.absent();
    }

    public PlayerWidgetRemoteViews build(Context context) {
        PlayerWidgetRemoteViews widgetRemoteView = new PlayerWidgetRemoteViews(context);

        if (!optionalIsPlaying.isPresent() && !optionalItem.isPresent()) {
            setEmptyState(context, widgetRemoteView);
        }

        if (optionalIsPlaying.isPresent()) {
            setPlaybackStatus(widgetRemoteView);
        }

        if (optionalItem.isPresent()) {
            setPlayableProperties(context, widgetRemoteView);
        }

        if (optionalArtwork != null) {
            if (optionalArtwork.isPresent()) {
                widgetRemoteView.setImageViewBitmap(R.id.icon, optionalArtwork.get());
            } else {
                widgetRemoteView.setImageViewResource(R.id.icon, R.drawable.appwidget_artwork_placeholder);
            }
        }

        return widgetRemoteView;
    }

    private void setPlayableProperties(Context context, PlayerWidgetRemoteViews widgetRemoteView) {
        WidgetItem item = optionalItem.get();
        widgetRemoteView.setCurrentTrackTitle(item.getTitle());
        widgetRemoteView.setCurrentCreator(item.getCreatorName());
        setLikeProperties(widgetRemoteView, item);
        widgetRemoteView.linkPlayControls(context, item.isPlayableFromWidget());
        widgetRemoteView.linkTitles(context, item.getUrn(), item.getCreatorUrn());
        widgetRemoteView.linkLikeToggle(context, item.isUserLike());
    }

    private void setLikeProperties(PlayerWidgetRemoteViews widgetRemoteView, WidgetItem item) {
        final boolean isLikeable = item.isUserLike().isPresent();
        widgetRemoteView.setLikeShown(isLikeable);
        if (isLikeable) {
            boolean isUserLike = item.isUserLike().get();
            widgetRemoteView.setImageViewResource(R.id.btn_like, isUserLike
                                                                 ?
                                                                 R.drawable.widget_like_orange :
                                                                 R.drawable.widget_like_grey);
        }
    }

    private void setPlaybackStatus(PlayerWidgetRemoteViews widgetRemoteView) {
        widgetRemoteView.setPlaybackStatus(optionalIsPlaying.get());
    }

    private void setEmptyState(Context context, PlayerWidgetRemoteViews widgetRemoteView) {
        widgetRemoteView.setEmptyState(context);
    }

    PlayerWidgetRemoteViewsBuilder forItem(WidgetItem widgetItem) {
        this.optionalItem = Optional.fromNullable(widgetItem);
        if (optionalItem.isPresent() && !optionalItem.get().hasArtwork()) {
            optionalArtwork = Optional.absent();
        }
        return this;
    }

    PlayerWidgetRemoteViewsBuilder forArtwork(Bitmap artwork) {
        this.optionalArtwork = Optional.fromNullable(artwork);
        return this;
    }

    PlayerWidgetRemoteViewsBuilder forIsPlaying(WidgetItem item, boolean isPlaying) {
        this.optionalIsPlaying = Optional.of(isPlaying);
        this.optionalItem = Optional.of(item);
        return this;
    }

}
