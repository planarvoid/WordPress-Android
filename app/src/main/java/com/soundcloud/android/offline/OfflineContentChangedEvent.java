package com.soundcloud.android.offline;

import static java.util.Collections.singletonList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;
import rx.functions.Func1;

import java.util.Collection;
import java.util.Collections;

public final class OfflineContentChangedEvent {

    public static final Func1<OfflineContentChangedEvent, OfflineState> TO_OFFLINE_STATE = event -> event.state;

    public static final Func1<OfflineContentChangedEvent, Boolean> HAS_LIKED_COLLECTION_CHANGE = event -> event.isLikedTrackCollection;

    static final Func1<OfflineContentChangedEvent, Boolean> TO_LIKES_COLLECTION_MARKED_OFFLINE = event -> event.state != OfflineState.NOT_OFFLINE;

    public final OfflineState state;
    public final Collection<Urn> entities;
    public final boolean isLikedTrackCollection;

    OfflineContentChangedEvent(OfflineState state, Collection<Urn> entities, boolean isLikedTrackCollection) {
        this.state = state;
        this.entities = entities;
        this.isLikedTrackCollection = isLikedTrackCollection;
    }

    public static OfflineContentChangedEvent requested(Collection<Urn> entities, boolean isLikedTrackCollection) {
        return new OfflineContentChangedEvent(OfflineState.REQUESTED, entities, isLikedTrackCollection);
    }

    public static OfflineContentChangedEvent downloading(Collection<Urn> entities, boolean isLikedTrackCollection) {
        return new OfflineContentChangedEvent(OfflineState.DOWNLOADING, entities, isLikedTrackCollection);
    }

    public static OfflineContentChangedEvent downloaded(Collection<Urn> entities, boolean isLikedTrackCollection) {
        return new OfflineContentChangedEvent(OfflineState.DOWNLOADED, entities, isLikedTrackCollection);
    }

    public static OfflineContentChangedEvent unavailable(Collection<Urn> entities, boolean isLikedTrackCollection) {
        return new OfflineContentChangedEvent(OfflineState.UNAVAILABLE, entities, isLikedTrackCollection);
    }

    public static OfflineContentChangedEvent removed(Urn entity) {
        return removed(singletonList(entity));
    }

    public static OfflineContentChangedEvent removed(Collection<Urn> entities) {
        return new OfflineContentChangedEvent(OfflineState.NOT_OFFLINE, entities, false);
    }

    public static OfflineContentChangedEvent removed(boolean isLikedTrackCollection) {
        return new OfflineContentChangedEvent(OfflineState.NOT_OFFLINE,
                                              Collections.emptyList(),
                                              isLikedTrackCollection);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OfflineContentChangedEvent that = (OfflineContentChangedEvent) o;
        return MoreObjects.equal(isLikedTrackCollection, that.isLikedTrackCollection) &&
                MoreObjects.equal(state, that.state) &&
                MoreObjects.equal(entities, that.entities);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(state, entities, isLikedTrackCollection);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("state", state)
                          .add("entities", entities)
                          .add("isLikedTrackCollection", isLikedTrackCollection)
                          .toString();
    }

}
