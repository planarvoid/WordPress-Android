package com.soundcloud.android.playback;

import static com.soundcloud.android.model.Urn.forTrack;
import static com.soundcloud.android.model.Urn.forUser;
import static com.soundcloud.android.storage.Tables.PlayQueue.QUERY_URN;
import static com.soundcloud.android.storage.Tables.PlayQueue.REPOSTER_ID;
import static com.soundcloud.android.storage.Tables.PlayQueue.SOURCE;
import static com.soundcloud.android.storage.Tables.PlayQueue.SOURCE_URN;
import static com.soundcloud.android.storage.Tables.PlayQueue.SOURCE_VERSION;
import static com.soundcloud.android.storage.Tables.PlayQueue.TRACK_ID;
import static com.soundcloud.android.testsupport.PlayQueueAssertions.assertPlayQueueItemsEqual;
import static com.soundcloud.propeller.query.Query.from;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackQueueItem.Builder;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.test.assertions.QueryAssertions;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;

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
        insertPlayableQueueItem(new Builder(forTrack(1))
                .fromSource("existing", "existing_version", new Urn("existing_sourceUrn"), new Urn("existing_queryUrn"))
                .build());

        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(1);

        TestObserver<TxnResult> observer = new TestObserver<>();
        EntityQueueItem entityQueueItem1 = new Builder(forTrack(123L))
                .fromSource("source1", "version1", new Urn("sourceUrn1"), new Urn("queryUrn1"))
                .relatedEntity(RELATED_ENTITY)
                .build();

        EntityQueueItem entityQueueItem2 = new Builder(forTrack(456L), forUser(456L))
                .fromSource("source2", "version2", new Urn("sourceUrn2"), new Urn("queryUrn2"))
                .build();

        PlayQueue playQueue = new PlayQueue(Arrays.<PlayQueueItem>asList(entityQueueItem1, entityQueueItem2));

        storage.storeAsync(playQueue).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);

        TxnResult txnResult = observer.getOnNextEvents().get(0);
        assertThat(txnResult.success()).isTrue();

        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(2);
        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE)
                .whereEq(TRACK_ID, 123L)
                .whereNull(REPOSTER_ID)
                .whereEq(Tables.PlayQueue.RELATED_ENTITY, RELATED_ENTITY.toString())
                .whereEq(SOURCE, "source1")
                .whereEq(SOURCE_VERSION, "version1")
                .whereEq(SOURCE_URN, "sourceUrn1")
                .whereEq(QUERY_URN, "queryUrn1"))).counts(1);

        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE)
                .whereEq(TRACK_ID, 456L)
                .whereEq(REPOSTER_ID, 456L)
                .whereEq(SOURCE, "source2")
                .whereEq(SOURCE_VERSION, "version2")
                .whereEq(SOURCE_URN, "sourceUrn2")
                .whereEq(QUERY_URN, "queryUrn2"))).counts(1);
    }

    @Test
    public void shouldSavePersistantItems() {
        final EntityQueueItem entityQueueItem1 = new Builder(forTrack(1), forUser(1))
                .fromSource("source1", "version1", new Urn("sourceUrn1"), new Urn("queryUrn1"))
                .persist(true)
                .build();
        final EntityQueueItem entityQueueItem2 = new Builder(forTrack(2), forUser(2))
                .fromSource("source2", "version2", new Urn("sourceUrn2"), new Urn("queryUrn2"))
                .persist(false)
                .build();
        PlayQueue playQueue = new PlayQueue(Arrays.<PlayQueueItem>asList(entityQueueItem1, entityQueueItem2));

        storage.storeAsync(playQueue).subscribe(new TestObserver<TxnResult>());

        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(1);
    }

    @Test
    public void shouldDeleteAllPlayQueueItems() {
        TestSubscriber<ChangeResult> subscriber = new TestSubscriber<>();
        insertPlayableQueueItem(new Builder(forTrack(123L), forUser(123L))
                .fromSource("source", "source_version", new Urn("sourceUrn"), new Urn("queryUrn"))
                .build());
        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(1);

        storage.clearAsync().subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getNumRowsAffected()).isEqualTo(1);
        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).isEmpty();
    }

    @Test
    public void shouldLoadAllPlayQueueItems() {
        TestSubscriber<PlayQueueItem> subscriber = new TestSubscriber<>();
        final EntityQueueItem expectedItem = new Builder(forTrack(123L), forUser(123L))
                .fromSource("source", "source_version", new Urn("sourceUrn"), new Urn("queryUrn"))
                .relatedEntity(RELATED_ENTITY)
                .build();
        insertPlayableQueueItem(expectedItem);
        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(1);

        storage.loadAsync().subscribe(subscriber);

        assertPlayQueueItemsEqual(Arrays.asList(expectedItem), subscriber.getOnNextEvents());
    }

    @Test
    public void shouldLoadAllPlayQueueItemsWithoutReposter() {
        TestSubscriber<PlayQueueItem> subscriber = new TestSubscriber<>();
        final EntityQueueItem expectedItem = new Builder(forTrack(123L))
                .fromSource("source", "source_version", new Urn("sourceUrn"), new Urn("queryUrn"))
                .relatedEntity(RELATED_ENTITY)
                .build();
        insertPlayableQueueItem(expectedItem);
        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(1);

        storage.loadAsync().subscribe(subscriber);

        assertPlayQueueItemsEqual(Arrays.asList(expectedItem), subscriber.getOnNextEvents());
    }

    @Test
    public void shouldLoadAllPlayQueueItemsWithoutRelatedEntities() {
        TestSubscriber<PlayQueueItem> subscriber = new TestSubscriber<>();
        final EntityQueueItem expectedItem = new Builder(forTrack(123L), forTrack(123L))
                .fromSource("source", "source_version", new Urn("sourceUrn"), new Urn("queryUrn"))
                .build();
        insertPlayableQueueItem(expectedItem);
        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(1);

        storage.loadAsync().subscribe(subscriber);

        assertPlayQueueItemsEqual(Arrays.asList(expectedItem), subscriber.getOnNextEvents());
    }

    @Test
    public void shouldLoadAllPlayQueueItemsWithoutSourceOrQueryUrn() {
        TestSubscriber<PlayQueueItem> subscriber = new TestSubscriber<>();
        final EntityQueueItem expectedItem = new Builder(forTrack(123L), forTrack(123L))
                // From a source with no source_urn or query_urn
                .fromSource("source", "source_version")
                .build();
        insertPlayableQueueItem(expectedItem);
        QueryAssertions.assertThat(select(from(PLAY_QUEUE_TABLE))).counts(1);

        storage.loadAsync().subscribe(subscriber);

        assertPlayQueueItemsEqual(Arrays.asList(expectedItem), subscriber.getOnNextEvents());
    }

    private void insertPlayableQueueItem(EntityQueueItem entityQueueItem) {
        ContentValues cv = new ContentValues();
        cv.put(Tables.PlayQueue.TRACK_ID.name(), entityQueueItem.getUrn().getNumericId());
        cv.put(Tables.PlayQueue.SOURCE.name(), entityQueueItem.getSource());
        cv.put(Tables.PlayQueue.SOURCE_VERSION.name(), entityQueueItem.getSourceVersion());
        cv.put(Tables.PlayQueue.SOURCE_URN.name(), entityQueueItem.getSourceUrn().toString());
        cv.put(Tables.PlayQueue.QUERY_URN.name(), entityQueueItem.getQueryUrn().toString());

        if (!Urn.NOT_SET.equals(entityQueueItem.getRelatedEntity())){
            cv.put(Tables.PlayQueue.RELATED_ENTITY.name(), entityQueueItem.getRelatedEntity().toString());
        }

        if (entityQueueItem.getReposter().isUser()){
            cv.put(Tables.PlayQueue.REPOSTER_ID.name(), entityQueueItem.getReposter().getNumericId());
        }

        testFixtures().insertInto(Tables.PlayQueue.TABLE, cv);
    }
}
