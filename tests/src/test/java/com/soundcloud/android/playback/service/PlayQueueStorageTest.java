package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.PlayQueueStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.DBHelper;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import javax.inject.Inject;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueStorageTest {

    @Inject
    PlayQueueStorage playQueueStorage;

    @Mock
    ContentResolver contentResolver;

    @Before
    public void setUp() throws Exception {
        ObjectGraph.create(new TestModule()).inject(this);
    }

    @Test
    public void clearStateShouldDeletePlayQueueUri() throws Exception {
        playQueueStorage.clearState();
        verify(contentResolver).delete(Content.PLAY_QUEUE.uri, null, null);
    }

    @Test
    public void storeShouldBulkInsertAllTrackIdsAndSources() throws Exception {
        PlayQueueItem playQueueItem1 = new PlayQueueItem(1L, "source1", "version1");
        PlayQueueItem playQueueItem2 = new PlayQueueItem(2L, "source2", "version2");
        playQueueStorage.storeCollection(Lists.newArrayList(playQueueItem1, playQueueItem2));

        ArgumentCaptor<ContentValues[]> captor = ArgumentCaptor.forClass(ContentValues[].class);
        verify(contentResolver).bulkInsert(any(Uri.class), captor.capture());

        final ContentValues[] value = captor.getValue();

        expect(value[0].get(DBHelper.PlayQueue.TRACK_ID)).toEqual(1L);
        expect(value[0].get(DBHelper.PlayQueue.SOURCE)).toEqual("source1");
        expect(value[0].get(DBHelper.PlayQueue.SOURCE_VERSION)).toEqual("version1");

        expect(value[1].get(DBHelper.PlayQueue.TRACK_ID)).toEqual(2L);
        expect(value[1].get(DBHelper.PlayQueue.SOURCE)).toEqual("source2");
        expect(value[1].get(DBHelper.PlayQueue.SOURCE_VERSION)).toEqual("version2");
    }

    @Test
    public void getPlayQueueItemsShouldQueryForAllOnPlayQueueUri() throws Exception {
        playQueueStorage.getPlayQueueItems();
        verify(contentResolver).query(Content.PLAY_QUEUE.uri, null, null, null, null);

    }

    @Module(injects = PlayQueueStorageTest.class)
    public class TestModule {
        @Provides
        ContentResolver provideContentResolver() {
            return contentResolver;
        }

    }
}
