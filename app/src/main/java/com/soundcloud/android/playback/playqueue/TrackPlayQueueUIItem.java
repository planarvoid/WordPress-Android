package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.tracks.TieredTracks;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import rx.functions.Func1;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;

class TrackPlayQueueUIItem extends PlayQueueUIItem {

    enum PlayState {PLAYING, COMING_UP, PLAYED}

    static final Predicate<PlayQueueUIItem> IS_TRACK = new Predicate<PlayQueueUIItem>() {
        @Override
        public boolean apply(PlayQueueUIItem input) {
            return input.isTrack();
        }
    };

    static final Func1<PlayQueueUIItem, Boolean> ONLY_TRACKS = new Func1<PlayQueueUIItem, Boolean>() {
        @Override
        public Boolean call(PlayQueueUIItem playQueueUIItem) {
            return playQueueUIItem.isTrack();
        }
    };

    private final PlayQueueItem playQueueItem;
    private final TrackItem trackItem;
    private final int uniqueId;
    private final int statusLabelId;
    private final ImageResource imageResource;
    private final int titleTextColor;
    private final Optional<String> contextTitle;
    private PlayQueueManager.RepeatMode repeatMode;
    private PlayState currentPlayingState;

    TrackPlayQueueUIItem(PlayQueueItem playQueueItem,
                         TrackItem trackItem,
                         int uniqueId,
                         int statusLabelId,
                         @ColorInt int titleTextColor,
                         ImageResource imageResource,
                         Optional<String> contextTitle,
                         PlayQueueManager.RepeatMode repeatMode) {
        this.playQueueItem = playQueueItem;
        this.trackItem = trackItem;
        this.uniqueId = uniqueId;
        this.statusLabelId = statusLabelId;
        this.imageResource = imageResource;
        this.titleTextColor = titleTextColor;
        this.contextTitle = contextTitle;
        this.repeatMode = repeatMode;
        this.currentPlayingState = PlayState.COMING_UP;
    }

    static TrackPlayQueueUIItem from(PlayQueueItem playQueueItem,
                                     TrackItem trackItem,
                                     Context context,
                                     Optional<String> contextTitle,
                                     PlayQueueManager.RepeatMode repeatMode) {
        return new TrackPlayQueueUIItem(
                playQueueItem,
                trackItem,
                System.identityHashCode(playQueueItem),
                createStatusLabelId(trackItem),
                getColor(trackItem.isBlocked(), context),
                getImageResource(trackItem),
                contextTitle,
                repeatMode
        );
    }

    @Override
    Kind getKind() {
        return Kind.TRACK;
    }

    @Override
    long getUniqueId() {
        return uniqueId;
    }

    public TrackItem getTrackItem() {
        return trackItem;
    }

    public PlayQueueItem getPlayQueueItem() {
        return playQueueItem;
    }


    public Urn getUrn() {
        return playQueueItem.getUrn();
    }

    public String getTitle() {
        return trackItem.getTitle();
    }

    public Optional<String> getContextTitle() {
        return contextTitle;
    }

    public String getCreator() {
        return trackItem.getCreatorName();
    }

    int getStatusLabelId() {
        return statusLabelId;
    }

    public boolean isBlocked() {
        return trackItem.isBlocked();
    }

    public ImageResource getImageResource() {
        return imageResource;
    }

    PlayQueueManager.RepeatMode getRepeatMode() {
        return repeatMode;
    }

    void setRepeatMode(PlayQueueManager.RepeatMode repeatMode) {
        this.repeatMode = repeatMode;
    }

    PlayState getPlayState() {
        return currentPlayingState;
    }

    public void setPlayState(PlayState currentPlayingState) {
        this.currentPlayingState = currentPlayingState;
    }

    @ColorInt
    public int getTitleTextColor() {
        return titleTextColor;
    }

    private static ImageResource getImageResource(TrackItem trackItem) {
        Urn urn = trackItem.getUrn();
        PropertySet propertySet = trackItem.getSource();
        Optional<String> templateUrl = propertySet.getOrElse(EntityProperty.IMAGE_URL_TEMPLATE,
                                                             Optional.<String>absent());
        return SimpleImageResource.create(urn, templateUrl);
    }

    private static int createStatusLabelId(TrackItem trackItem) {
        if (trackItem.isBlocked()) {
            return R.layout.not_available;
        } else if (TieredTracks.isHighTierPreview(trackItem)) {
            return R.layout.preview;
        } else if (TieredTracks.isFullHighTierTrack(trackItem)) {
            return R.layout.go_label;
        } else if (trackItem.isPrivate()) {
            return R.layout.private_label;
        } else {
            return Consts.NOT_SET;
        }
    }

    private static int getColor(boolean blocked, Context context) {
        if (blocked) {
            return ContextCompat.getColor(context, R.color.ak_medium_dark_gray);
        } else {
            return ContextCompat.getColor(context, R.color.ak_light_gray);
        }
    }

}
