package com.soundcloud.android.tracks;

import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;

class TrackDetails {

    private final TrackUrn urn;
    private final String title;
    private final String creator;
    private final String description;

    TrackDetails(PropertySet propertySet) {
        this.urn = propertySet.get(TrackProperty.URN);
        this.title = propertySet.get(PlayableProperty.TITLE);
        this.creator = propertySet.get(PlayableProperty.CREATOR_NAME);
        this.description = ScTextUtils.EMPTY_STRING;
    }

    TrackDetails(PublicApiTrack track) {
        this.urn = track.getUrn();
        this.title = track.getTitle();
        this.creator = track.getUsername();
        this.description = track.description;
    }

    public TrackUrn getUrn() {
        return urn;
    }

    public String getTitle() {
        return title;
    }

    public String getCreator() {
        return creator;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "TrackDetails{" +
                "urn=" + urn +
                ", title='" + title + '\'' +
                ", creator='" + creator + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
