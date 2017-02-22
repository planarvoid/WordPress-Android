package com.soundcloud.android.playback.widget;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;

class WidgetItem implements ImageResource {

    private String title;
    private Optional<String> creatorName = Optional.absent();
    private Optional<Urn> creatorUrn = Optional.absent();
    private Optional<Urn> urn = Optional.absent();
    private Optional<String> imageUrlTemplate = Optional.absent();
    private Optional<Boolean> isUserLike = Optional.absent();
    private boolean isPlayableFromWidget = true;

    private WidgetItem(String title, String creatorName, Urn creatorUrn, Urn urn,
                       Optional<String> imageUrlTemplate, boolean isUserLike) {
        this.title = title;
        this.creatorName = Optional.of(creatorName);
        this.creatorUrn = Optional.of(creatorUrn);
        this.urn = Optional.of(urn);
        this.imageUrlTemplate = imageUrlTemplate;
        this.isUserLike = Optional.of(isUserLike);
    }

    private WidgetItem(String title, boolean isPlayableFromWidget) {
        this.title = title;
        this.isPlayableFromWidget = isPlayableFromWidget;
    }

    public static WidgetItem fromTrackItem(TrackItem trackItem) {
        return new WidgetItem(trackItem.title(),
                              trackItem.creatorName(),
                              trackItem.creatorUrn(),
                              trackItem.getUrn(),
                              trackItem.getImageUrlTemplate(),
                              trackItem.isLikedByCurrentUser());
    }

    public static WidgetItem forAudioAd(Resources res) {
        return new WidgetItem(res.getString(R.string.ads_advertisement), true);
    }

    public static WidgetItem forVideoAd(Resources res) {
        return new WidgetItem(res.getString(R.string.ads_reopen_to_continue_short), false);
    }

    @Override
    public Urn getUrn() {
        return urn.or(Urn.NOT_SET);
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return imageUrlTemplate;
    }

    String getTitle() {
        return title;
    }

    String getCreatorName() {
        return creatorName.or(Strings.EMPTY);
    }

    Optional<Urn> getCreatorUrn() {
        return creatorUrn;
    }

    Optional<Boolean> isUserLike() {
        return isUserLike;
    }

    boolean isPlayableFromWidget() {
        return isPlayableFromWidget;
    }

    public boolean hasArtwork() {
        return urn.isPresent();
    }

}
