package com.soundcloud.android.sync;

import static com.soundcloud.android.storage.Table.Collections;
import static com.soundcloud.android.storage.TableColumns.Collections.LAST_SYNC;
import static com.soundcloud.android.storage.TableColumns.Collections.LAST_SYNC_ATTEMPT;
import static com.soundcloud.android.storage.TableColumns.Collections.URI;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.propeller.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.fakes.RoboSharedPreferences;
import rx.observers.TestSubscriber;

import android.content.Context;
import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

public class SyncStateStorageTest extends StorageIntegrationTest {

    private static final SyncContent SYNC_CONTENT = SyncContent.MySoundStream;
    private static final Uri CONTENT_URI = SYNC_CONTENT.content.uri;
    private SyncStateStorage storage;
    private TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
    private TestSubscriber<Long> timeSubscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        RoboSharedPreferences preferences = new RoboSharedPreferences(new HashMap<String, Map<String, Object>>(), "TEST", Context.MODE_PRIVATE);
        storage = new SyncStateStorage(propeller(), preferences, new TestDateProvider());
    }

    @Test
    public void hasSyncedBeforeIsTrueWithSuccessfulSyncDataStored() {
        testFixtures().insertSuccessfulSync(SYNC_CONTENT, 123L);

        storage.hasSyncedBefore(CONTENT_URI).subscribe(subscriber);

        subscriber.assertValues(true);
    }

    @Test
    public void hasSyncedBeforeIsFalseWithNoSuccessfulSyncStored() {
        testFixtures().insertSyncAttempt(SYNC_CONTENT, 123L);

        storage.hasSyncedBefore(CONTENT_URI).subscribe(subscriber);

        subscriber.assertValues(false);
    }

    @Test
    public void hasSyncedBeforeIsFalseWithNoSyncDataStored() {
        storage.hasSyncedBefore(CONTENT_URI).subscribe(subscriber);

        subscriber.assertValues(false);
    }

    @Test
    public void hasSyncedBeforeShouldReturnFalseWhenNotSynced() {
        assertThat(storage.hasSyncedBefore("test")).isFalse();
    }

    @Test
    public void hasSyncedBeforeShouldReturnTrueWhenSynced() {
        storage.synced("test");
        assertThat(storage.hasSyncedBefore("test")).isTrue();
    }

    @Test
    public void lastSyncOrAttemptTimeShouldReturnLastTimestampOnSucessfulSync() {
        testFixtures().insertSuccessfulSync(SYNC_CONTENT, 123L);

        storage.lastSyncOrAttemptTime(CONTENT_URI).subscribe(timeSubscriber);

        timeSubscriber.assertValue(123L);
    }

    @Test
    public void lastSyncOrAttemptTimeShouldReturnLastAttemptTimestampOnFailedSync() {
        testFixtures().insertSyncAttemptAndLast(SYNC_CONTENT, 234L, 123L);

        storage.lastSyncOrAttemptTime(CONTENT_URI).subscribe(timeSubscriber);

        timeSubscriber.assertValue(234L);
    }

    @Test
    public void lastSyncOrAttemptTimeShouldReturnLastTimestampOnSuccess() {
        testFixtures().insertSyncAttemptAndLast(SYNC_CONTENT, 123L, 234L);

        storage.lastSyncOrAttemptTime(CONTENT_URI).subscribe(timeSubscriber);

        timeSubscriber.assertValue(234L);
    }

    @Test
    public void lastSyncOrAttemptTimeShouldReturnNotSetTheFirstTime() {
        storage.lastSyncOrAttemptTime(CONTENT_URI).subscribe(timeSubscriber);

        timeSubscriber.assertValue(-1L);
    }

    @Test
    public void shouldLoadSyncMissesFromExtraField() {
        testFixtures().insertSyncState(CONTENT_URI, 123, 456, "5");

        assertThat(storage.legacyLoadSyncMisses(CONTENT_URI)).isEqualTo(5);
    }

    @Test
    public void shouldLoadSyncMissesAsZeroWhenSyncStateAbsent() {
        assertThat(storage.legacyLoadSyncMisses(CONTENT_URI)).isEqualTo(0);
    }

    @Test
    public void shouldLoadSyncMissesAsZeroWhenMissesAbsent() {
        testFixtures().insertSyncState(CONTENT_URI, 123, 456, "");

        assertThat(storage.legacyLoadSyncMisses(CONTENT_URI)).isEqualTo(0);
    }

    @Test
    public void shouldUpdateSyncMissesForPresentState() {
        testFixtures().insertSyncState(CONTENT_URI, 123, 456, "5");

        storage.legacyUpdateSyncMisses(CONTENT_URI, 6);

        assertThat(storage.legacyLoadSyncMisses(CONTENT_URI)).isEqualTo(6);
    }

    @Test
    public void shouldUpdateSyncMissesForAbsentState() {
        storage.legacyUpdateSyncMisses(CONTENT_URI, 6);

        assertThat(storage.legacyLoadSyncMisses(CONTENT_URI)).isEqualTo(6);
    }

    @Test
    public void shouldUpdateLastSyncAttemptForPresentState() {
        testFixtures().insertSyncState(CONTENT_URI, 123, 456, "5");

        storage.legacyUpdateLastSyncAttempt(CONTENT_URI, 789);

        assertThat(propeller().query(Query
                        .from(Collections)
                        .select(LAST_SYNC_ATTEMPT)
                        .whereEq(URI, CONTENT_URI))
                        .firstOrDefault(Long.class, 0L)
        ).isEqualTo(789);
    }

    @Test
    public void shouldUpdateLastSyncAttemptForAbsentState() {
        storage.legacyUpdateLastSyncAttempt(CONTENT_URI, 789);

        assertThat(propeller().query(Query
                        .from(Collections)
                        .select(LAST_SYNC_ATTEMPT)
                        .whereEq(URI, CONTENT_URI))
                        .firstOrDefault(Long.class, 0L)
        ).isEqualTo(789);
    }

    @Test
    public void shouldUpdateLastSyncSuccessForPresentState() {
        testFixtures().insertSyncState(CONTENT_URI, 123, 456, "5");

        storage.legacyUpdateLastSyncSuccess(CONTENT_URI, 789);

        assertThat(propeller().query(Query
                        .from(Collections)
                        .select(LAST_SYNC)
                        .whereEq(URI, CONTENT_URI))
                        .firstOrDefault(Long.class, 0L)
        ).isEqualTo(789);
    }

    @Test
    public void shouldUpdateLastSyncSuccessForAbsentState() {
        storage.legacyUpdateLastSyncSuccess(CONTENT_URI, 789);

        assertThat(propeller().query(Query
                        .from(Collections)
                        .select(LAST_SYNC)
                        .whereEq(URI, CONTENT_URI))
                        .firstOrDefault(Long.class, 0L)
        ).isEqualTo(789);
    }

    @Test
    public void shouldIgnoreUriQueryStrings() {
        storage.legacyUpdateLastSyncSuccess(CONTENT_URI.buildUpon()
                .appendQueryParameter("a", "b").build(), 1L);

        assertThat(storage.legacyLoadLastSyncSuccess(CONTENT_URI.buildUpon()
                .appendQueryParameter("c", "d").build())).isEqualTo(1);
    }
}
