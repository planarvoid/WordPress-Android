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

import java.util.List;

class SearchItem {

    private final Urn urn;
    private final Optional<PropertySet> source;

    private SearchItem(Urn urn) {
        this.urn = buildItemUrn(urn);
        this.source = absent();
    }

    private SearchItem(PropertySet propertySet) {
        Preconditions.checkNotNull(propertySet);
        this.urn = buildItemUrn(propertySet.get(EntityProperty.URN));
        this.source = Optional.of(propertySet);
    }

    private Urn buildItemUrn(Urn urn) {
        return (urn != null) ? urn : Urn.NOT_SET;
    }

    static SearchItem fromUrn(Urn urn) {
        return new SearchItem(urn);
    }

    static SearchItem fromPropertySet(PropertySet propertySet) {
        return new SearchItem(propertySet);
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
        return urn.equals(Urn.NOT_SET);
    }

    ListItem build() {
        if (source.isPresent()) {
            if (isTrack()) {
                return TrackItem.from(source.get());
            } else if (isPlaylist()) {
                return PlaylistItem.from(source.get());
            } else if (isUser()) {
                return UserItem.from(source.get());
            }
        }
        throw new IllegalArgumentException("ListItem type not valid");
    }

    static ListItem buildPremiumItem(List<PropertySet> propertySets) {
        return new SearchPremiumItem(propertySets);
    }
}
