package com.soundcloud.android.events;

import static com.soundcloud.android.events.UrnStateChangedEvent.Kind.ENTITY_CREATED;
import static com.soundcloud.android.events.UrnStateChangedEvent.Kind.ENTITY_DELETED;
import static com.soundcloud.android.events.UrnStateChangedEvent.Kind.STATIONS_COLLECTION_UPDATED;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Sets;

import java.util.Set;

@AutoValue
public abstract class UrnStateChangedEvent {
    public enum Kind {
        ENTITY_CREATED, ENTITY_DELETED, STATIONS_COLLECTION_UPDATED
    }

    public abstract Kind kind();

    public abstract Set<Urn> urns();


    public boolean containsPlaylist() {
        for (Urn urn : urns()) {
            if (urn.isPlaylist()) {
                return true;
            }
        }
        return false;
    }

    public boolean containsCreatedPlaylist() {
        if (kind() != ENTITY_CREATED) {
            return false;
        }
        for (Urn urn : urns()) {
            if (urn.isPlaylist()) {
                return true;
            }
        }
        return false;
    }

    public boolean containsDeletedPlaylist() {
        if (kind() != ENTITY_DELETED) {
            return false;
        }
        for (Urn urn : urns()) {
            if (urn.isPlaylist()) {
                return true;
            }
        }
        return false;
    }

    public static UrnStateChangedEvent fromEntityCreated(Urn urn) {
        return new AutoValue_UrnStateChangedEvent(ENTITY_CREATED, Sets.newHashSet(urn));
    }

    public static UrnStateChangedEvent fromEntitiesCreated(Set<Urn> urns) {
        return new AutoValue_UrnStateChangedEvent(ENTITY_CREATED, urns);
    }

    public static UrnStateChangedEvent fromEntityDeleted(Urn urn) {
        return new AutoValue_UrnStateChangedEvent(ENTITY_DELETED, Sets.newHashSet(urn));
    }

    public static UrnStateChangedEvent fromEntitiesDeleted(Set<Urn> urns) {
        return new AutoValue_UrnStateChangedEvent(ENTITY_DELETED, urns);
    }

    public static UrnStateChangedEvent fromStationsUpdated(Urn station) {
        return new AutoValue_UrnStateChangedEvent(STATIONS_COLLECTION_UPDATED, Sets.newHashSet(station));
    }

}
