package com.soundcloud.android.search;

import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

import java.util.List;

class SearchPremiumItem implements ListItem {

    static final Urn PREMIUM_URN = new Urn("local:search:premium");

    private final List<ListItem> sourceSet;
    private final Optional<Link> nextHref;
    private final int resultsCount;

    private final ListItem firstItem;

    SearchPremiumItem(List<ListItem> sourceSetPremiumItems, Optional<Link> nextHref, int resultsCount) {
        this.sourceSet = sourceSetPremiumItems;
        this.nextHref = nextHref;
        this.resultsCount = resultsCount;
        this.firstItem = buildFirstListItem(sourceSetPremiumItems);
    }

    private ListItem buildFirstListItem(List<ListItem> premiumItems) {
        return premiumItems.get(0);
    }

    @Override
    public Urn getUrn() {
        return PREMIUM_URN;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return Optional.absent();
    }

    List<ListItem> getSourceSet() {
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
        if (firstItem.getUrn().isTrack()) {
            final TrackItem trackItem = (TrackItem) firstItem;
            trackItem.setIsPlaying(trackItem.getUrn().equals(currentlyPlayingUrn));
        }
    }
}
