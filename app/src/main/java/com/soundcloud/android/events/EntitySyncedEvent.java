package com.soundcloud.android.events;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropertySet;

import android.support.v4.util.ArrayMap;

import java.util.Collection;
import java.util.Map;

public final class EntitySyncedEvent {

    private final Map<Urn, PropertySet> changeSet;

    public EntitySyncedEvent(Collection<PropertySet> changeSet) {
        this.changeSet = new ArrayMap<>(changeSet.size());
        for (PropertySet propertySet : changeSet){
            this.changeSet.put(propertySet.get(EntityProperty.URN), propertySet);
        }
    }

    public Map<Urn, PropertySet> getChangeSet() {
        return changeSet;
    }

}
