package com.soundcloud.android.stations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

class StationInfoTrack implements ListItem {

    private Urn urn;
    private String title;
    private String creator;
    private Urn creatorUrn;
    private Optional<String> imageUrlTemplate;
    private boolean isPlaying;

    StationInfoTrack(Urn urn, String title, String creator, Urn creatorUrn, Optional<String> imageUrlTemplate) {
        this.urn = urn;
        this.title = title;
        this.creator = creator;
        this.creatorUrn = creatorUrn;
        this.imageUrlTemplate = imageUrlTemplate;
    }

    @Override
    public ListItem update(PropertySet sourceSet) {
        // TODO:
        return null;
    }

    @Override
    public Urn getUrn() {
        return urn;
    }

    public String getTitle() {
        return title;
    }

    public String getCreator() {
        return creator;
    }

    public Urn getCreatorUrn() {
        return creatorUrn;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return imageUrlTemplate;
    }

    public void setIsPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationInfoTrack that = (StationInfoTrack) o;
        return MoreObjects.equal(urn, that.urn) &&
                MoreObjects.equal(title, that.title) &&
                MoreObjects.equal(creator, that.creator) &&
                MoreObjects.equal(creatorUrn, that.creatorUrn) &&
                MoreObjects.equal(imageUrlTemplate, that.imageUrlTemplate);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(urn, title, creator, creatorUrn, imageUrlTemplate);
    }
}
