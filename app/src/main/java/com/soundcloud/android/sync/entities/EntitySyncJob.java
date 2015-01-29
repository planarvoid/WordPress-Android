package com.soundcloud.android.sync.entities;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.ApiResourceCommand;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class EntitySyncJob implements SyncJob {

    private static final String TAG = "EntitySyncJob";

    private final ApiResourceCommand<List<Urn>, ModelCollection<PropertySetSource>> fetchResources;
    private final StoreCommand storeResources;
    private final Function<PropertySetSource, PropertySet> apiModelCollectionToPropSet = new Function<PropertySetSource, PropertySet>() {
        @Override
        public PropertySet apply(PropertySetSource propertySetSource) {
            return propertySetSource.toPropertySet();
        }
    };

    private List<Urn> urns = Collections.emptyList();
    private Collection<PropertySet> updatedPropertySets = Collections.emptyList();

    @Inject
    public EntitySyncJob(ApiResourceCommand fetchResources, StoreCommand storeResources) {
        this.fetchResources = fetchResources;
        this.storeResources = storeResources;
    }

    public void setUrns(List<Urn> urns) {
        this.urns = urns;
    }

    public Collection<PropertySet> getUpdatedEntities(){
        return updatedPropertySets;
    }

    @Override
    public void run() {
        try {
            if (!urns.isEmpty()) {
                ModelCollection<PropertySetSource> collection = fetchResources.with(urns).call();
                storeResources.with(collection).call();
                updatedPropertySets = Collections2.transform(collection.getCollection(), apiModelCollectionToPropSet);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating resources ", e);
        }
    }


    @Override
    public void onQueued() {
        // no-op
    }

    @Override
    public boolean resultedInAChange() {
        return false; // unused
    }

    @Override
    public Exception getException() {
        return null; // unused
    }
}
