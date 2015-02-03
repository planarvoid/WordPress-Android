package com.soundcloud.android.sync.entities;

import com.google.common.collect.Collections2;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.GuavaFunctions;
import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class EntitySyncJob implements SyncJob {

    private final BulkFetchCommand<PropertySetSource> fetchResources;
    private final StoreCommand storeResources;

    private List<Urn> urns = Collections.emptyList();
    private Collection<PropertySet> updatedPropertySets = Collections.emptyList();

    @Inject
    public EntitySyncJob(BulkFetchCommand fetchResources, StoreCommand storeResources) {
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
                updatedPropertySets = Collections2.transform(collection.getCollection(), GuavaFunctions.PROP_SET_SOURCE_TO_PROP_SET);
            }
        } catch (Exception e) {
            ErrorUtils.handleThrowable(e, this.getClass());
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
