package com.soundcloud.android.events;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropertySet;

import android.support.v4.util.ArrayMap;

import java.util.Collection;
import java.util.Map;

public final class EntityUpdatedEvent {

    private final Map<Urn, PropertySet> changeMap;

    public EntityUpdatedEvent(Collection<PropertySet> changedEntities) {
        this.changeMap = new ArrayMap<>(changedEntities.size());
        for (PropertySet entity : changedEntities){
            this.changeMap.put(entity.get(EntityProperty.URN), entity);
        }
    }

    public Map<Urn, PropertySet> getChangeMap() {
        return changeMap;
    }

}
