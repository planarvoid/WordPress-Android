package com.soundcloud.android.search;

import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.UpdatableItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import java.util.List;

class SearchPremiumItem implements ListItem, UpdatableItem {

    static final Urn PREMIUM_URN = new Urn("local:search:premium");

    private final List<PropertySet> sourceSet;
    private final Optional<Link> nextHref;
    private final int resultsCount;

    private final ListItem firstItem;

    SearchPremiumItem(List<PropertySet> sourceSetPremiumItems, Optional<Link> nextHref, int resultsCount) {
        this.sourceSet = sourceSetPremiumItems;
        this.nextHref = nextHref;
        this.resultsCount = resultsCount;
        this.firstItem = buildFirstListItem(sourceSetPremiumItems);
    }

    private ListItem buildFirstListItem(List<PropertySet> premiumItems) {
        final PropertySet firstItem = premiumItems.get(0);
        final SearchResultItem searchResultItem = SearchResultItem.fromUrn(firstItem.get(EntityProperty.URN));
        ListItem listItem = null;
        if (searchResultItem.isTrack()) {
            listItem = TrackItem.from(firstItem);
        } else if (searchResultItem.isPlaylist()) {
            listItem = PlaylistItem.from(firstItem);
        } else if (searchResultItem.isUser()) {
            listItem = UserItem.from(firstItem);
        }
        return listItem;
    }

    @Override
    public SearchPremiumItem updated(PropertySet updatedProperties) {
        for (PropertySet propertySet : sourceSet) {
            if (propertySet.get(EntityProperty.URN).equals(updatedProperties.get(EntityProperty.URN))) {
                propertySet.update(updatedProperties);
                return this;
            }
        }
        return this;
    }

    @Override
    public Urn getUrn() {
        return PREMIUM_URN;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return Optional.absent();
    }

    List<PropertySet> getSourceSet() {
        return sourceSet;
    }

    Optional<Link> getNextHref() {
        return nextHref;
    }

    int getResultsCount() {
        return resultsCount;
    }

    ListItem getFirstItem() {
        return firstItem;
    }

    void setTrackIsPlaying(Urn currentlyPlayingUrn) {
        if (SearchResultItem.fromUrn(firstItem.getUrn()).isTrack()) {
            final TrackItem trackItem = (TrackItem) firstItem;
            trackItem.setIsPlaying(trackItem.getUrn().equals(currentlyPlayingUrn));
        }
    }
}
