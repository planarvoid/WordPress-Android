package com.soundcloud.android.search;

import static com.soundcloud.java.optional.Optional.absent;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

class SearchResultItem {

    private final Urn urn;
    private final Optional<PropertySet> source;

    private SearchResultItem(Urn urn) {
        this.urn = buildItemUrn(urn);
        this.source = absent();
    }

    private SearchResultItem(PropertySet propertySet) {
        Preconditions.checkNotNull(propertySet);
        this.urn = buildItemUrn(propertySet.get(EntityProperty.URN));
        this.source = Optional.of(propertySet);
    }

    private Urn buildItemUrn(Urn urn) {
        return (urn != null) ? urn : Urn.NOT_SET;
    }

    static SearchResultItem fromUrn(Urn urn) {
        return new SearchResultItem(urn);
    }

    static SearchResultItem fromPropertySet(PropertySet propertySet) {
        return new SearchResultItem(propertySet);
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
        return urn.equals(SearchUpsellItem.UPSELL_URN);
    }

    ListItem build() {
        if (source.isPresent()) {
            if (isTrack()) {
                return TrackItem.from(source.get());
            } else if (isPlaylist()) {
                return PlaylistItem.from(source.get());
            } else if (isUser()) {
                return UserItem.from(source.get());
            } else if (isUpsell()) {
                return new SearchUpsellItem();
            }
        }
        throw new IllegalArgumentException("ListItem type not valid");
    }
}
