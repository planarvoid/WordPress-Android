package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import rx.functions.Func1;

import android.support.v4.util.ArrayMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@AutoValue
public abstract class EntityStateChangedEvent implements UrnEvent {

    public static final int UPDATED = 0;

    public static final Func1<EntityStateChangedEvent, Urn> TO_URN = entityStateChangedEvent -> entityStateChangedEvent.getFirstUrn();

    public static EntityStateChangedEvent mergeUpdates(Collection<EntityStateChangedEvent> entityStateChangedEvents) {
        final Map<Urn, PropertySet> merged = new HashMap<>();
        for (EntityStateChangedEvent event : entityStateChangedEvents) {
            merged.putAll(event.getChangeMap());
        }
        return new AutoValue_EntityStateChangedEvent(UPDATED, merged);
    }

    public static EntityStateChangedEvent forUpdate(PropertySet propertySet) {
        return create(UPDATED, propertySet);
    }

    private static EntityStateChangedEvent create(int kind, Collection<PropertySet> changedEntities) {
        Map<Urn, PropertySet> changeMap = new ArrayMap<>(changedEntities.size());
        for (PropertySet entity : changedEntities) {
            changeMap.put(entity.get(EntityProperty.URN), entity);
        }
        return new AutoValue_EntityStateChangedEvent(kind, changeMap);
    }

    private static EntityStateChangedEvent create(int kind, PropertySet changedEntity) {
        return create(kind, Collections.singleton(changedEntity));
    }

    public abstract int getKind();

    public abstract Map<Urn, PropertySet> getChangeMap();

    public Urn getFirstUrn() {
        return getChangeMap().keySet().iterator().next();
    }

    /**
     * @return for a single change event, this returns the single change set; if more than one entity changed,
     * returns the first available change set.
     */
    public PropertySet getNextChangeSet() {
        return getChangeMap().values().iterator().next();
    }

    public boolean isSingularChange() {
        return getChangeMap().size() == 1;
    }

    public boolean containsTrackChange() {
        for (Urn urn : getChangeMap().keySet()) {
            if (urn.isTrack()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("kind", getKind()).add("changeMap", getChangeMap()).toString();
    }

}
