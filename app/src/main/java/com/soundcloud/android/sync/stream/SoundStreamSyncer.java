package com.soundcloud.android.sync.stream;

import static com.soundcloud.android.sync.SyncModule.SOUND_STREAM_SYNC_STORAGE;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.TimelineSyncStorage;
import com.soundcloud.android.sync.TimelineSyncer;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;
import javax.inject.Named;

public class SoundStreamSyncer extends TimelineSyncer<ApiStreamItem> {
    @Inject
    public SoundStreamSyncer(ApiClient apiClient,
                             StoreSoundStreamCommand storeSoundStreamCommand,
                             ReplaceSoundStreamCommand replaceSoundStreamCommand,
                             @Named(SOUND_STREAM_SYNC_STORAGE) TimelineSyncStorage timelineSyncStorage) {
        super(ApiEndpoints.STREAM, Content.ME_SOUND_STREAM.uri, apiClient, storeSoundStreamCommand,
                replaceSoundStreamCommand, timelineSyncStorage,
                new TypeToken<ModelCollection<ApiStreamItem>>() {
                });
    }
}
