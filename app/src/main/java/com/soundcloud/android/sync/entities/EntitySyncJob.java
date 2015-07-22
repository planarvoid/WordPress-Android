package com.soundcloud.android.sync.entities;

import com.google.common.collect.Collections2;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.GuavaFunctions;
import com.soundcloud.java.collections.PropertySet;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class EntitySyncJob implements SyncJob {

    private final BulkFetchCommand<PropertySetSource> fetchResources;
    private final WriteStorageCommand storeResources;

    private List<Urn> urns = Collections.emptyList();
    private Collection<PropertySet> updatedPropertySets = Collections.emptyList();
    private Exception exception;

    @Inject
    public EntitySyncJob(BulkFetchCommand fetchResources, WriteStorageCommand storeResources) {
        this.fetchResources = fetchResources;
        this.storeResources = storeResources;
    }

    public void setUrns(List<Urn> urns) {
        this.urns = validUrns(urns);
    }

    public Collection<PropertySet> getUpdatedEntities(){
        return updatedPropertySets;
    }

    @Override
    public void run() {
        try {
            if (!urns.isEmpty()) {
                Collection<PropertySetSource> collection = fetchResources.with(urns).call();
                storeResources.call(collection);
                updatedPropertySets = Collections2.transform(collection, GuavaFunctions.toPropertySet());
            }
        } catch (Exception e) {
            ErrorUtils.handleThrowable(e, this.getClass());
            exception = e;
        }
    }

    private List<Urn> validUrns(List<Urn> urns) {
        List<Urn> validUrns = new ArrayList<>(urns.size());
        for (Urn urn : urns){
            if (urn.getNumericId() > 0){
                validUrns.add(urn);
            }
        }
        return validUrns;
    }


    @Override
    public void onQueued() {
        // no-op
    }

    @Override
    public boolean resultedInAChange() {
        return exception == null;
    }

    @Override
    public Exception getException() {
        return exception;
    }
}
