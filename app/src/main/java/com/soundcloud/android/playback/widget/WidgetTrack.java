package com.soundcloud.android.playback.widget;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

class WidgetTrack implements ImageResource{

    private final PropertySet source;

    WidgetTrack(PropertySet source) {
        this.source = source;
    }

    @Override
    public Urn getUrn() {
        return source.get(TrackProperty.URN);
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return source.get(TrackProperty.IMAGE_URL_TEMPLATE);
    }

    String getTitle() {
        return source.get(PlayableProperty.TITLE);
    }

    String getUserName() {
        return source.get(PlayableProperty.CREATOR_NAME);
    }

    Urn getUserUrn() {
        return source.get(PlayableProperty.CREATOR_URN);
    }

    boolean isUserLike() {
        return source.get(PlayableProperty.IS_USER_LIKE);
    }

    boolean isAudioAd() {
        return source.get(AdProperty.IS_AUDIO_AD);
    }
}
