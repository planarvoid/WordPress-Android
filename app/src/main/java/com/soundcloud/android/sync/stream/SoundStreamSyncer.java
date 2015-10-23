package com.soundcloud.android.sync.stream;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.TimelineSyncer;
import com.soundcloud.java.reflect.TypeToken;

public class SoundStreamSyncer extends TimelineSyncer<ApiStreamItem> {
    public SoundStreamSyncer(ApiClient apiClient,
                             Command<Iterable<ApiStreamItem>, ?> storeSoundStreamCommand,
                             Command<Iterable<ApiStreamItem>, ?> replaceSoundStreamCommand,
                             StreamSyncStorage streamSyncStorage) {
        super(ApiEndpoints.STREAM, Content.ME_SOUND_STREAM.uri, apiClient, storeSoundStreamCommand,
                replaceSoundStreamCommand, streamSyncStorage,
                new TypeToken<ModelCollection<ApiStreamItem>>() {
                });
    }
}
