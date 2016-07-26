package com.soundcloud.android.sync.activities;

import static com.soundcloud.android.sync.SyncModule.ACTIVITIES_SYNC_STORAGE;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.timeline.TimelineSyncStorage;
import com.soundcloud.android.sync.timeline.TimelineSyncer;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;
import javax.inject.Named;

public class ActivitiesSyncer extends TimelineSyncer<ApiActivityItem> {

    public ActivitiesSyncer(ApiClient apiClient,
                            StoreActivitiesCommand storeItemsCommand,
                            ReplaceActivitiesCommand replaceItemsCommand,
                            TimelineSyncStorage timelineSyncStorage,
                            String action) {
        super(ApiEndpoints.ACTIVITIES, Content.ME_ACTIVITIES.uri, apiClient, storeItemsCommand, replaceItemsCommand,
              timelineSyncStorage, new TypeToken<ModelCollection<ApiActivityItem>>() {
                }, action);
    }

    // AutoFactory does not support named dependencies : https://github.com/google/auto/issues/174
    public static class ActivitiesSyncerFactory {

        private final ApiClient apiClient;
        private final StoreActivitiesCommand storeActivitiesCommand;
        private final ReplaceActivitiesCommand replaceActivitiesCommand;
        private final TimelineSyncStorage timelineSyncStorage;

        @Inject
        public ActivitiesSyncerFactory(ApiClient apiClient,
                                        StoreActivitiesCommand storeActivitiesCommand,
                                        ReplaceActivitiesCommand replaceActivitiesCommand,
                                        @Named(ACTIVITIES_SYNC_STORAGE) TimelineSyncStorage timelineSyncStorage) {

            this.apiClient = apiClient;
            this.storeActivitiesCommand = storeActivitiesCommand;
            this.replaceActivitiesCommand = replaceActivitiesCommand;
            this.timelineSyncStorage = timelineSyncStorage;
        }

        public ActivitiesSyncer create(String action) {
            return new ActivitiesSyncer(apiClient,
                                         storeActivitiesCommand, replaceActivitiesCommand, timelineSyncStorage, action);
        }
    }
}
