package com.soundcloud.android.playback.service;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.PlayQueueStorage;
import com.soundcloud.android.storage.provider.Content;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.ContentResolver;

import javax.inject.Inject;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueStorageTest {

    @Inject
    PlayQueueStorage playQueueStorage;

    @Mock
    ContentResolver  contentResolver;

    @Before
    public void setUp() throws Exception {
        ObjectGraph.create(new TestModule()).inject(this);
    }

    @Test
    public void clearStateShouldDeletePlayQueueUri() throws Exception {
        playQueueStorage.clearState();
        verify(contentResolver).delete(Content.PLAY_QUEUE.uri, null, null);
    }

//    @Test
//    public void storeShouldBulkInsertAllTrackIdsAndSources() throws Exception {
//        playQueueStorage.store()
//
//    }

    @Module(injects = PlayQueueStorageTest.class)
    public class TestModule {
        @Provides
        ContentResolver provideContentResolver(){
            return contentResolver;
        }

    }
}
