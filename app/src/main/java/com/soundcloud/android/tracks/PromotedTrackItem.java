package com.soundcloud.android.tracks;

import com.google.common.base.Optional;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropertySet;

import java.util.List;

public class PromotedTrackItem extends TrackItem {

    public static PromotedTrackItem from(PropertySet promotedTrack) {
        return new PromotedTrackItem(promotedTrack);
    }

    PromotedTrackItem(PropertySet source) {
        super(source);
    }

    public String getAdUrn() {
        return source.get(PromotedTrackProperty.AD_URN);
    }

    public boolean hasPromoter() {
        return source.get(PromotedTrackProperty.PROMOTER_URN).isPresent();
    }

    public Optional<String> getPromoterName() {
        return source.get(PromotedTrackProperty.PROMOTER_NAME);
    }

    public Optional<Urn> getPromoterUrn() {
        return source.get(PromotedTrackProperty.PROMOTER_URN);
    }

    public List<String> getClickUrls() {
        return source.get(PromotedTrackProperty.TRACK_CLICKED_URLS);
    }

    public List<String> getImpressionUrls() {
        return source.get(PromotedTrackProperty.TRACK_IMPRESSION_URLS);
    }

    public List<String> getPromoterClickUrls() {
        return source.get(PromotedTrackProperty.PROMOTER_CLICKED_URLS);
    }

    public List<String> getPlayUrls() {
        return source.get(PromotedTrackProperty.TRACK_PLAYED_URLS);
    }

}
