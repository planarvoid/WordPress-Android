package com.soundcloud.android.events;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;

import android.support.v4.util.ArrayMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

public final class EntityStateChangedEvent {

    public static final int ENTITY_SYNCED = 0;
    public static final int DOWNLOAD_STARTED = 1;
    public static final int DOWNLOAD_FINISHED = 2;
    public static final int DOWNLOAD_FAILED = 3;

    private final int kind;
    private final Map<Urn, PropertySet> changeMap;

    public static EntityStateChangedEvent downloadStarted(Urn track) {
        return new EntityStateChangedEvent(DOWNLOAD_STARTED, Collections.singleton(
                PropertySet.from(
                        TrackProperty.URN.bind(track),
                        TrackProperty.OFFLINE_DOWNLOADING.bind(true))
        ));
    }

    public static EntityStateChangedEvent downloadFinished(Urn track) {
        return new EntityStateChangedEvent(DOWNLOAD_FINISHED, Collections.singleton(
                PropertySet.from(
                        TrackProperty.URN.bind(track),
                        TrackProperty.OFFLINE_DOWNLOADING.bind(false),
                        TrackProperty.OFFLINE_DOWNLOADED_AT.bind(new Date()))
        ));
    }

    public static EntityStateChangedEvent downloadFailed(Urn track) {
        return new EntityStateChangedEvent(DOWNLOAD_FAILED, Collections.singleton(
                PropertySet.from(
                        TrackProperty.URN.bind(track)
                        // TODO: This isn't handled yet anywhere, but will have to pick this up in views eventually
                )
        ));
    }

    public static EntityStateChangedEvent fromSync(Collection<PropertySet> changedEntities) {
        return new EntityStateChangedEvent(ENTITY_SYNCED, changedEntities);
    }

    EntityStateChangedEvent(int kind, Collection<PropertySet> changedEntities) {
        this.kind = kind;
        this.changeMap = new ArrayMap<>(changedEntities.size());
        for (PropertySet entity : changedEntities) {
            this.changeMap.put(entity.get(EntityProperty.URN), entity);
        }
    }

    public int getKind() {
        return kind;
    }

    public Map<Urn, PropertySet> getChangeMap() {
        return changeMap;
    }

}
