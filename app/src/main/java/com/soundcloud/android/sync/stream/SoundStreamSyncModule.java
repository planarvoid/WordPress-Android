package com.soundcloud.android.sync.stream;

import com.soundcloud.android.api.ApiClient;
import dagger.Module;
import dagger.Provides;

@Module(complete = false, library = true)
public class SoundStreamSyncModule {

    @Provides
    SoundStreamSyncer soundStreamSyncer(ApiClient apiClient,
                                        StoreSoundStreamCommand storeSoundStreamCommand,
                                        ReplaceSoundStreamCommand replaceSoundStreamCommand,
                                        StreamSyncStorage streamSyncStorage) {
        return new SoundStreamSyncer(apiClient, storeSoundStreamCommand, replaceSoundStreamCommand, streamSyncStorage);
    }

}
