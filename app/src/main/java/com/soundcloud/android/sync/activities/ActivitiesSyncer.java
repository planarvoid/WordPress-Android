package com.soundcloud.android.sync.activities;

import static com.soundcloud.android.sync.SyncModule.ACTIVITIES_SYNC_STORAGE;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.TimelineSyncStorage;
import com.soundcloud.android.sync.TimelineSyncer;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;
import javax.inject.Named;

public class ActivitiesSyncer extends TimelineSyncer<ApiActivityItem> {

    @Inject
    public ActivitiesSyncer(ApiClient apiClient,
                            StoreActivitiesCommand storeItemsCommand,
                            ReplaceActivitiesCommand replaceItemsCommand,
                            @Named(ACTIVITIES_SYNC_STORAGE) TimelineSyncStorage timelineSyncStorage) {
        super(ApiEndpoints.ACTIVITIES, Content.ME_ACTIVITIES.uri, apiClient, storeItemsCommand, replaceItemsCommand,
                timelineSyncStorage, new TypeToken<ModelCollection<ApiActivityItem>>() {
                });
    }
}
