package com.soundcloud.android.playback;

import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.storage.PlayQueueStorage;
import com.soundcloud.android.playback.service.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaybackService;
import dagger.Module;
import dagger.Provides;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;

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
    PlayQueueManager providePlayQueueManager(Context context, PlayQueueStorage playQueueStorage, PlaybackOperations playbackOperations,
                                             SharedPreferences sharedPreferences, ScModelManager modelManager) {
        return new PlayQueueManager(context, playQueueStorage, playbackOperations, sharedPreferences, modelManager);
    }

    @Provides
    PlayQueueStorage providePlayQueueStorage(ContentResolver contentResolver){
        return new PlayQueueStorage(contentResolver);
    }


}
