package com.soundcloud.android.playback.service;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.isEmpty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.StorageIntegrationTest;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.TxnResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.observers.TestObserver;

import android.content.ContentValues;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueStorageTest extends StorageIntegrationTest {

    private static final String PLAY_QUEUE_TABLE = Table.PLAY_QUEUE.name;

    private PlayQueueStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new PlayQueueStorage(testScheduler());
    }

    @Test
    public void shouldInsertPlayQueueAndReplaceExistingItems() {
        insertPlayQueueItem(PlayQueueItem.fromTrack(1L, "existing", "existing_version"));
        assertThat(select(from(PLAY_QUEUE_TABLE)), counts(1));

        TestObserver<TxnResult> observer = new TestObserver<TxnResult>();
        PlayQueueItem playQueueItem1 = PlayQueueItem.fromTrack(123L, "source1", "version1");
        PlayQueueItem playQueueItem2 = PlayQueueItem.fromTrack(456L, "source2", "version2");
        PlayQueue playQueue = new PlayQueue(Arrays.asList(playQueueItem1, playQueueItem2), 0);

        storage.storeAsync(playQueue).subscribe(observer);

        assertThat(observer.getOnNextEvents().size(), is(1));

        TxnResult txnResult = observer.getOnNextEvents().get(0);
        assertThat(txnResult.success(), is(true));

        assertThat(select(from(PLAY_QUEUE_TABLE)), counts(2));
        assertThat(select(from(PLAY_QUEUE_TABLE)
                .whereEq(TableColumns.PlayQueue.TRACK_ID, 123L)
                .whereEq(TableColumns.PlayQueue.SOURCE, "source1")
                .whereEq(TableColumns.PlayQueue.SOURCE_VERSION, "version1")), counts(1));
        assertThat(select(from(PLAY_QUEUE_TABLE)
                .whereEq(TableColumns.PlayQueue.TRACK_ID, 456L)
                .whereEq(TableColumns.PlayQueue.SOURCE, "source2")
                .whereEq(TableColumns.PlayQueue.SOURCE_VERSION, "version2")), counts(1));
    }

    @Test
    public void shouldDeleteAllPlayQueueItems() {
        TestObserver<ChangeResult> observer = new TestObserver<ChangeResult>();
        insertPlayQueueItem(PlayQueueItem.fromTrack(123L, "source", "source_version"));
        assertThat(select(from(PLAY_QUEUE_TABLE)), counts(1));

        storage.clearAsync().subscribe(observer);

        assertThat(observer.getOnNextEvents().size(), is(1));
        assertThat(observer.getOnNextEvents().get(0).getNumRowsAffected(), is(1));
        assertThat(select(from(PLAY_QUEUE_TABLE)), isEmpty());
    }

    @Test
    public void shouldLoadAllPlayQueueItems() {
        TestObserver<PlayQueueItem> observer = new TestObserver<PlayQueueItem>();
        final PlayQueueItem expectedItem = PlayQueueItem.fromTrack(123L, "source", "source_version");
        insertPlayQueueItem(expectedItem);
        assertThat(select(from(PLAY_QUEUE_TABLE)), counts(1));

        storage.loadAsync().subscribe(observer);

        assertThat(observer.getOnNextEvents().size(), is(1));
        PlayQueueItem item = observer.getOnNextEvents().get(0);
        assertThat(item, is(equalTo(expectedItem)));
    }

    private long insertPlayQueueItem(PlayQueueItem playQueueItem) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.PlayQueue.TRACK_ID, playQueueItem.getTrackId());
        cv.put(TableColumns.PlayQueue.SOURCE, playQueueItem.getSource());
        cv.put(TableColumns.PlayQueue.SOURCE_VERSION, playQueueItem.getSourceVersion());
        return testHelper().insertInto(Table.PLAY_QUEUE, cv);
    }
}
