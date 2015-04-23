package com.soundcloud.android.tracks;

import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropertySet;

public class PromotedTrackItem extends TrackItem {

    public static PromotedTrackItem from(PropertySet promotedTrack) {
        return new PromotedTrackItem(promotedTrack);
    }

    PromotedTrackItem(PropertySet source) {
        super(source);
    }

    public boolean hasPromoter() {
        return source.contains(PromotedTrackProperty.PROMOTER_URN);
    }

    public String getPromoterName() {
        return source.get(PromotedTrackProperty.PROMOTER_NAME).get();
    }

    public Urn getPromoterUrn() {
        return source.get(PromotedTrackProperty.PROMOTER_URN).get();
    }

}
