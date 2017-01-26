package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.search.SearchableItem;
import com.soundcloud.java.collections.Lists;

import java.util.List;

@AutoValue
public abstract class TopResultsBucketItem {
    public enum Kind {
        TOP_RESULT, TRACKS, GO_TRACKS, USERS, PLAYLISTS, ALBUMS
    }

    public abstract Kind kind();

    public abstract int totalResults();

    public abstract Urn queryUrn();

    public abstract List<SearchableItem> items();

    public static TopResultsBucketItem create(ApiTopResultsBucket topResults) {
        return new AutoValue_TopResultsBucketItem(urnToKind(topResults.urn()), topResults.totalResults(), topResults.queryUrn(), Lists.transform(topResults.collection().getCollection(),
                                                                                                                                                 ApiUniversalSearchItem::toSearchableItem));
    }

    private static Kind urnToKind(Urn urn) {
        switch (urn.getStringId()) {
            case "tracks":
                return Kind.TRACKS;
            default:
                throw new IllegalArgumentException("unexpected urn type for search");
        }
    }
}
