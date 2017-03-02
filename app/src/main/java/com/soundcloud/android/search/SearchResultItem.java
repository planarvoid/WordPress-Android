package com.soundcloud.android.search;

import com.soundcloud.android.model.Urn;

class SearchResultItem {

    private final Urn urn;

    private SearchResultItem(Urn urn) {
        this.urn = buildItemUrn(urn);
    }

    private Urn buildItemUrn(Urn urn) {
        return (urn != null) ? urn : Urn.NOT_SET;
    }

    static SearchResultItem fromUrn(Urn urn) {
        return new SearchResultItem(urn);
    }

    boolean isTrack() {
        return urn.isTrack();
    }

    boolean isPlaylist() {
        return urn.isPlaylist();
    }

    boolean isUser() {
        return urn.isUser();
    }

    boolean isPremiumContent() {
        return urn.equals(SearchPremiumItem.PREMIUM_URN);
    }

    boolean isUpsell() {
        return urn.equals(UpsellSearchableItem.UPSELL_URN);
    }
}
