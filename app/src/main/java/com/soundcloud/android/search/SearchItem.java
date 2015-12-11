package com.soundcloud.android.search;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.PropertySet;

import javax.annotation.Nullable;
import java.util.List;

class SearchItem {

    private final Urn urn;

    private SearchItem(Urn urn) {
        this.urn = urn;
    }

    static SearchItem fromUrn(Urn urn) {
        return (urn != null) ? new SearchItem(urn) : new SearchItem(Urn.NOT_SET);
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
        return (!isTrack() && !isUser() && !isPlaylist() && urn.equals(Urn.NOT_SET));
    }

    @Nullable ListItem build(PropertySet source) {
        if (isTrack()) {
            return TrackItem.from(source);
        } else if (isPlaylist()) {
            return PlaylistItem.from(source);
        } else if (isUser()) {
            return UserItem.from(source);
        }
        return null;
    }

    static ListItem buildPremiumItem(List<PropertySet> propertySets) {
        return new SearchPremiumItem(propertySets);
    }
}
