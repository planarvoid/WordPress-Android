package com.soundcloud.android.sync.entities;

import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.model.ApiSyncable;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.commands.PublishUpdateEventCommand;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntitySyncJob implements SyncJob {

    private final BulkFetchCommand<? extends ApiSyncable, ? extends ApiSyncable> fetchResources;
    private final WriteStorageCommand storeResources;
    private final Command<Collection<? extends ApiSyncable>, Boolean> publishSyncEvent;

    private List<Urn> urns = Collections.emptyList();
    private Set<Urn> uniqueUrns = Collections.emptySet();
    private Collection<? extends ApiSyncable> updatedEntities = Collections.emptyList();
    private Exception exception;

    @Inject
    public EntitySyncJob(BulkFetchCommand<? extends ApiSyncable, ? extends ApiSyncable> fetchResources,
                         WriteStorageCommand storeResources,
                         PublishUpdateEventCommand publishSyncEvent) {
        this.fetchResources = fetchResources;
        this.storeResources = storeResources;
        this.publishSyncEvent = publishSyncEvent;
    }

    public void setUrns(List<Urn> urns) {
        this.urns = urns;
        this.uniqueUrns = new HashSet<>(urns);
    }

    public void publishSyncEvent() {
        publishSyncEvent.call(updatedEntities);
    }

    @Override
    public void run() {
        try {
            if (!urns.isEmpty()) {
                updatedEntities = fetchResources.with(urns).call();
                storeResources.call(updatedEntities);
            }
        } catch (Exception e) {
            ErrorUtils.handleThrowable(e, this.getClass());
            exception = e;
        }
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

    @Override
    public Optional<Syncable> getSyncable() {
        return Optional.absent();
    }

    @Override
    public boolean wasSuccess() {
        return exception == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntitySyncJob that = (EntitySyncJob) o;

        return uniqueUrns.equals(that.uniqueUrns);
    }

    @Override
    public int hashCode() {
        return uniqueUrns.hashCode();
    }
}
