package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.StorageIntegrationTest;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.BulkInsertResult;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.observers.TestObserver;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueStorageTest extends StorageIntegrationTest {

    private PlayQueueStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new PlayQueueStorage(testScheduler());
    }

    @Test
    public void shouldInsertPlayQueueAndReplaceExistingItems() {
        testHelper().insertPlayQueueItem(new PlayQueueItem(1L, "existing", "existing_version"));
        expect(count(Table.PLAY_QUEUE.name)).toBe(1);

        TestObserver<BulkInsertResult> observer = new TestObserver<BulkInsertResult>();
        PlayQueueItem playQueueItem1 = new PlayQueueItem(123L, "source1", "version1");
        PlayQueueItem playQueueItem2 = new PlayQueueItem(456L, "source2", "version2");
        PlayQueue playQueue = new PlayQueue(Arrays.asList(playQueueItem1, playQueueItem2), 0);

        storage.storeAsync(playQueue).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);

        BulkInsertResult insertResult = observer.getOnNextEvents().get(0);
        expect(insertResult.success()).toBeTrue();
        expect(count(Table.PLAY_QUEUE.name)).toBe(2);
        expect(exists(Query.from(Table.PLAY_QUEUE.name)
                .whereEq(TableColumns.PlayQueue.TRACK_ID, 123L)
                .whereEq(TableColumns.PlayQueue.SOURCE, "source1")
                .whereEq(TableColumns.PlayQueue.SOURCE_VERSION, "version1"))).toBeTrue();
        expect(exists(Query.from(Table.PLAY_QUEUE.name)
                .whereEq(TableColumns.PlayQueue.TRACK_ID, 456L)
                .whereEq(TableColumns.PlayQueue.SOURCE, "source2")
                .whereEq(TableColumns.PlayQueue.SOURCE_VERSION, "version2"))).toBeTrue();
    }

    @Test
    public void shouldDeleteAllPlayQueueItems() {
        TestObserver<ChangeResult> observer = new TestObserver<ChangeResult>();
        testHelper().insertPlayQueueItem(new PlayQueueItem(123L, "source", "source_version"));
        expect(count(Table.PLAY_QUEUE.name)).toBe(1);

        storage.clearAsync().subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).getNumRowsAffected()).toBe(1);
        expect(count(Table.PLAY_QUEUE.name)).toBe(0);
    }

    @Test
    public void shouldLoadAllPlayQueueItems() {
        TestObserver<PlayQueueItem> observer = new TestObserver<PlayQueueItem>();
        final PlayQueueItem expectedItem = new PlayQueueItem(123L, "source", "source_version");
        testHelper().insertPlayQueueItem(expectedItem);
        expect(count(Table.PLAY_QUEUE.name)).toBe(1);

        storage.loadAsync().subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        PlayQueueItem item = observer.getOnNextEvents().get(0);
        expect(item).toEqual(expectedItem);
    }
}
