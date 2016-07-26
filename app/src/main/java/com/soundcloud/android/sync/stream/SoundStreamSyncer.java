package com.soundcloud.android.sync.stream;

import static com.soundcloud.android.sync.SyncModule.SOUND_STREAM_SYNC_STORAGE;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.timeline.TimelineSyncStorage;
import com.soundcloud.android.sync.timeline.TimelineSyncer;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;
import javax.inject.Named;

public class SoundStreamSyncer extends TimelineSyncer<ApiStreamItem> {

    public SoundStreamSyncer(ApiClient apiClient,
                             StoreSoundStreamCommand storeSoundStreamCommand,
                             ReplaceSoundStreamCommand replaceSoundStreamCommand,
                             TimelineSyncStorage timelineSyncStorage,
                             String action) {
        super(ApiEndpoints.STREAM, Content.ME_SOUND_STREAM.uri, apiClient, storeSoundStreamCommand,
              replaceSoundStreamCommand, timelineSyncStorage,
              new TypeToken<ModelCollection<ApiStreamItem>>() {
              }, action);
    }

    // AutoFactory does not support named dependencies : https://github.com/google/auto/issues/174
    public static class SoundStreamSyncerFactory {
        private final ApiClient apiClient;
        private final StoreSoundStreamCommand storeSoundStreamCommand;
        private final ReplaceSoundStreamCommand replaceSoundStreamCommand;
        private final TimelineSyncStorage timelineSyncStorage;

        @Inject
        public SoundStreamSyncerFactory(ApiClient apiClient,
                                 StoreSoundStreamCommand storeSoundStreamCommand,
                                 ReplaceSoundStreamCommand replaceSoundStreamCommand,
                                 @Named(SOUND_STREAM_SYNC_STORAGE) TimelineSyncStorage timelineSyncStorage) {

            this.apiClient = apiClient;
            this.storeSoundStreamCommand = storeSoundStreamCommand;
            this.replaceSoundStreamCommand = replaceSoundStreamCommand;
            this.timelineSyncStorage = timelineSyncStorage;
        }

        public SoundStreamSyncer create(String action) {
            return new SoundStreamSyncer(apiClient, storeSoundStreamCommand, replaceSoundStreamCommand, timelineSyncStorage, action);
        }
    }
}
