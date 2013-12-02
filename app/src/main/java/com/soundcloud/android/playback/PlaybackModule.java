package com.soundcloud.android.playback;

import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlayQueueOperations;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.storage.PlayQueueStorage;
import dagger.Module;
import dagger.Provides;

import android.content.ContentResolver;
import android.content.Context;

import javax.inject.Singleton;

@Module(complete = false, library = true, injects = PlaybackService.class)
public class PlaybackModule {

    @Provides
    @Singleton
    PlaybackOperations providePlaybackOperations() {
        return new PlaybackOperations();
    }

    @Provides
    @Singleton
    PlayQueueOperations providePlayQueueOperations(Context context, PlayQueueStorage playQueueStorage, RxHttpClient rxHttpClient) {
        return new PlayQueueOperations(context, playQueueStorage, rxHttpClient);
    }

    @Provides
    @Singleton
    PlayQueueManager providePlayQueueManager(Context context, PlayQueueOperations playQueueOperations, ScModelManager modelManager) {
        return new PlayQueueManager(context, playQueueOperations, modelManager);
    }

    @Provides
    PlayQueueStorage providePlayQueueStorage(ContentResolver contentResolver){
        return new PlayQueueStorage(contentResolver);
    }


}
