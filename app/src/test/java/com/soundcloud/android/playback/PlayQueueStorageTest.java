package com.soundcloud.android.playback;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.isEmpty;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.TxnResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestObserver;

import android.content.ContentValues;

import java.util.Arrays;

public class PlayQueueStorageTest extends StorageIntegrationTest {

    private static final com.soundcloud.propeller.schema.Table PLAY_QUEUE_TABLE = Tables.PlayQueue.TABLE;
    private static final Urn RELATED_ENTITY = Urn.forTrack(987L);

    private PlayQueueStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new PlayQueueStorage(propellerRx());
    }

    @Test
    public void shouldInsertPlayQueueAndReplaceExistingItems() {
        insertPlayQueueItem(new PlayQueueItem.Builder(Urn.forTrack(1))
                .fromSource("existing", "existing_version")
                .build());

        Assert.assertThat(select(from(PLAY_QUEUE_TABLE)), counts(1));

        TestObserver<TxnResult> observer = new TestObserver<>();
        PlayQueueItem playQueueItem1 = new PlayQueueItem.Builder(Urn.forTrack(123L))
                .fromSource("source1", "version1")
                .relatedEntity(RELATED_ENTITY)
                .build();

        PlayQueueItem playQueueItem2 = new PlayQueueItem.Builder(Urn.forTrack(456L), Urn.forUser(456L))
                .fromSource("source2", "version2")
                .build();

        PlayQueue playQueue = new PlayQueue(Arrays.asList(playQueueItem1, playQueueItem2));

        storage.storeAsync(playQueue).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);

        TxnResult txnResult = observer.getOnNextEvents().get(0);
        assertThat(txnResult.success()).isTrue();

        Assert.assertThat(select(from(PLAY_QUEUE_TABLE)), counts(2));
        Assert.assertThat(select(from(PLAY_QUEUE_TABLE)
                .whereEq(Tables.PlayQueue.TRACK_ID, 123L)
                .whereNull(Tables.PlayQueue.REPOSTER_ID)
                .whereEq(Tables.PlayQueue.RELATED_ENTITY, RELATED_ENTITY.toString())
                .whereEq(Tables.PlayQueue.SOURCE, "source1")
                .whereEq(Tables.PlayQueue.SOURCE_VERSION, "version1")), counts(1));

        Assert.assertThat(select(from(PLAY_QUEUE_TABLE)
                .whereEq(Tables.PlayQueue.TRACK_ID, 456L)
                .whereEq(Tables.PlayQueue.REPOSTER_ID, 456L)
                .whereEq(Tables.PlayQueue.SOURCE, "source2")
                .whereEq(Tables.PlayQueue.SOURCE_VERSION, "version2")), counts(1));
    }

    @Test
    public void shouldSavePersistantItems() {
        final PlayQueueItem playQueueItem1 = new PlayQueueItem.Builder(Urn.forTrack(1), Urn.forUser(1))
                .fromSource("source1", "version1")
                .withAdData(PropertySet.create())
                .persist(true)
                .build();
        final PlayQueueItem playQueueItem2 = new PlayQueueItem.Builder(Urn.forTrack(2), Urn.forUser(2))
                .fromSource("source2", "version2")
                .withAdData(PropertySet.create())
                .persist(false)
                .build();
        PlayQueue playQueue = new PlayQueue(Arrays.asList(playQueueItem1, playQueueItem2));

        storage.storeAsync(playQueue).subscribe(new TestObserver<TxnResult>());

        Assert.assertThat(select(from(PLAY_QUEUE_TABLE)), counts(1));
    }

    @Test
    public void shouldDeleteAllPlayQueueItems() {
        TestObserver<ChangeResult> observer = new TestObserver<>();
        insertPlayQueueItem(new PlayQueueItem.Builder(Urn.forTrack(123L), Urn.forUser(123L))
                .fromSource("source", "source_version")
                .build());
        Assert.assertThat(select(from(PLAY_QUEUE_TABLE)), counts(1));

        storage.clearAsync().subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).getNumRowsAffected()).isEqualTo(1);
        Assert.assertThat(select(from(PLAY_QUEUE_TABLE)), isEmpty());
    }

    @Test
    public void shouldLoadAllPlayQueueItems() {
        TestObserver<PlayQueueItem> observer = new TestObserver<>();
        final PlayQueueItem expectedItem = new PlayQueueItem.Builder(Urn.forTrack(123L), Urn.forUser(123L))
                .fromSource("source", "source_version")
                .relatedEntity(RELATED_ENTITY)
                .build();
        insertPlayQueueItem(expectedItem);
        Assert.assertThat(select(from(PLAY_QUEUE_TABLE)), counts(1));

        storage.loadAsync().subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        PlayQueueItem item = observer.getOnNextEvents().get(0);
        assertThat(item).isEqualTo(expectedItem);
    }

    @Test
    public void shouldLoadAllPlayQueueItemsWithoutReposter() {
        TestObserver<PlayQueueItem> observer = new TestObserver<>();
        final PlayQueueItem expectedItem = new PlayQueueItem.Builder(Urn.forTrack(123L))
                .fromSource("source", "source_version")
                .relatedEntity(RELATED_ENTITY)
                .build();
        insertPlayQueueItem(expectedItem);
        Assert.assertThat(select(from(PLAY_QUEUE_TABLE)), counts(1));

        storage.loadAsync().subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        PlayQueueItem item = observer.getOnNextEvents().get(0);
        assertThat(item).isEqualTo(expectedItem);
    }

    @Test
    public void shouldLoadAllPlayQueueItemsWithoutRelatedEntities() {
        TestObserver<PlayQueueItem> observer = new TestObserver<>();
        final PlayQueueItem expectedItem = new PlayQueueItem.Builder(Urn.forTrack(123L), Urn.forTrack(123L))
                .fromSource("source", "source_version")
                .build();
        insertPlayQueueItem(expectedItem);
        Assert.assertThat(select(from(PLAY_QUEUE_TABLE)), counts(1));

        storage.loadAsync().subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        PlayQueueItem item = observer.getOnNextEvents().get(0);
        assertThat(item).isEqualTo(expectedItem);
    }

    private void insertPlayQueueItem(PlayQueueItem playQueueItem) {
        ContentValues cv = new ContentValues();
        cv.put(Tables.PlayQueue.TRACK_ID.name(), playQueueItem.getTrackUrn().getNumericId());
        cv.put(Tables.PlayQueue.SOURCE.name(), playQueueItem.getSource());
        cv.put(Tables.PlayQueue.SOURCE_VERSION.name(), playQueueItem.getSourceVersion());

        if (!Urn.NOT_SET.equals(playQueueItem.getRelatedEntity())){
            cv.put(Tables.PlayQueue.RELATED_ENTITY.name(), playQueueItem.getRelatedEntity().toString());
        }

        if (playQueueItem.getReposter().isUser()){
            cv.put(Tables.PlayQueue.REPOSTER_ID.name(), playQueueItem.getReposter().getNumericId());
        }

        testFixtures().insertInto(Tables.PlayQueue.TABLE, cv);
    }
}
