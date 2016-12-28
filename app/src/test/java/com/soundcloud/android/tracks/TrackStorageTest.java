package com.soundcloud.android.tracks;

import static com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.schema.BulkInsertValues;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class TrackStorageTest extends StorageIntegrationTest {

    private static final int BATCH_TRACKS_COUNT = 1500;
    private TrackStorage storage;

    @Before
    public void setup() {
        storage = new TrackStorage(propellerRx());
    }

    @Test
    public void loadsTrack() {
        ApiTrack apiTrack = testFixtures().insertTrack();

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        assertThat(TrackItem.from(track)).isEqualTo(TestPropertySets.fromApiTrack(apiTrack));
    }

    @Test
    public void loadsDownloadedTrack() {
        ApiTrack apiTrack = testFixtures().insertTrack();
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), 0, 1000L);

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        final TrackItem expected = TestPropertySets.fromApiTrack(apiTrack);
        expected.setOfflineState(OfflineState.DOWNLOADED);
        assertThat(TrackItem.from(track)).isEqualTo(expected);
    }

    @Test
    public void loadsPendingRemovalTrack() {
        ApiTrack apiTrack = testFixtures().insertTrack();
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 2000L);

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        final TrackItem expected = TestPropertySets.fromApiTrack(apiTrack);
        expected.setOfflineState(OfflineState.NOT_OFFLINE);
        assertThat(TrackItem.from(track)).isEqualTo(expected);
    }

    @Test
    public void loadsBlockedTrack() {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setBlocked(true);
        testFixtures().insertTrack(apiTrack);

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        assertThat(TrackItem.from(track)).isEqualTo(TestPropertySets.fromApiTrack(apiTrack));
    }

    @Test
    public void loadsSnippedTrack() {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setSnipped(true);
        testFixtures().insertTrack(apiTrack);

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        assertThat(TrackItem.from(track)).isEqualTo(TestPropertySets.fromApiTrack(apiTrack));
    }

    @Test
    public void doesntCrashOnNullWaveforms() {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setWaveformUrl(null);
        testFixtures().insertTrack(apiTrack);

        storage.loadTrack(apiTrack.getUrn());
    }

    @Test
    public void loadLikedTrack() {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date());

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        assertThat(track.get(PlayableProperty.IS_USER_LIKE)).isTrue();
    }

    @Test
    public void loadUnlikedTrack() {
        ApiTrack apiTrack = testFixtures().insertTrack();

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        assertThat(track.get(PlayableProperty.IS_USER_LIKE)).isFalse();
    }

    @Test
    public void shouldReturnEmptyPropertySetIfTrackNotFound() {
        PropertySet track = storage.loadTrack(Urn.forTrack(123)).toBlocking().single();

        assertThat(track).isEqualTo(PropertySet.create());
    }

    @Test
    public void descriptionByUrnEmitsInsertedDescription() {
        ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertDescription(track.getUrn(), "description123");

        PropertySet description = storage.loadTrackDescription(track.getUrn()).toBlocking().single();

        assertThat(description).isEqualTo(PropertySet.from(TrackProperty.DESCRIPTION.bind("description123")));
    }

    @Test
    public void availableTracksReturnsEmptyWhenStorageIsEmpty() {
        final TestSubscriber<List<Urn>> subscriber = new TestSubscriber<>();

        storage.availableTracks(singletonList(Urn.forTrack(133L))).subscribe(subscriber);

        subscriber.assertValue(Collections.<Urn>emptyList());
    }

    @Test
    public void availableTracksReturnsEmptyWhenNoneIsPresent() {
        final TestSubscriber<List<Urn>> subscriber = new TestSubscriber<>();
        testFixtures().insertTrack();

        storage.availableTracks(singletonList(Urn.forTrack(731982L))).subscribe(subscriber);

        subscriber.assertValue(Collections.<Urn>emptyList());
    }

    @Test
    public void availableTracksReturnsPresentTracks() {
        final TestSubscriber<List<Urn>> subscriber = new TestSubscriber<>();
        ApiTrack apiTrack = testFixtures().insertTrack();

        storage.availableTracks(asList(apiTrack.getUrn(), Urn.forTrack(731982L))).subscribe(subscriber);

        subscriber.assertValue(singletonList(apiTrack.getUrn()));
    }

    @Test
    public void loadTracksSetsUserLikeIndividually() {
        final TestSubscriber<Map<Urn, TrackItem>> subscriber = new TestSubscriber<>();
        ApiTrack likedApiTrack = testFixtures().insertLikedTrack(new Date());
        ApiTrack apiTrack = testFixtures().insertTrack();

        storage.loadTracks(asList(likedApiTrack.getUrn(), apiTrack.getUrn())).subscribe(subscriber);
        Map<Urn, TrackItem> map = subscriber.getOnNextEvents().get(0);

        assertThat(map.get(likedApiTrack.getUrn()).isLikedByCurrentUser()).isTrue();
        assertThat(map.get(apiTrack.getUrn()).isLikedByCurrentUser()).isFalse();
    }

    @Test
    public void loadTracksSetsUserRepostsIndividually() {
        final TestSubscriber<Map<Urn, TrackItem>> subscriber = new TestSubscriber<>();
        ApiTrack postedApiTrack = testFixtures().insertPostedTrack(new Date(), true);
        ApiTrack apiTrack = testFixtures().insertTrack();

        storage.loadTracks(asList(postedApiTrack.getUrn(), apiTrack.getUrn())).subscribe(subscriber);
        Map<Urn, TrackItem> map = subscriber.getOnNextEvents().get(0);

        assertThat(map.get(postedApiTrack.getUrn()).isRepostedByCurrentUser()).isTrue();
        assertThat(map.get(apiTrack.getUrn()).isRepostedByCurrentUser()).isFalse();
    }

    @Test
    public void loadTracksInBatches() {
        final TestSubscriber<Map<Urn, TrackItem>> subscriber = new TestSubscriber<>();
        List<Urn> trackUrns = insertBulkTracks(BATCH_TRACKS_COUNT);

        storage.loadTracks(trackUrns).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents().get(0).keySet()).isEqualTo(new HashSet<>(trackUrns));
    }

    @Test
    public void loadAvailableTracksInBatches() {
        final TestSubscriber<List<Urn>> subscriber = new TestSubscriber<>();
        List<Urn> trackUrns = insertBulkTracks(BATCH_TRACKS_COUNT);

        storage.availableTracks(trackUrns).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(trackUrns);
    }

    // TODO: move to DatabaseFixtures?
    private List<Urn> insertBulkTracks(int count) {
        List<Urn> trackUrns = new ArrayList<>(count);

        BulkInsertValues.Builder trackBuilder = new BulkInsertValues.Builder(Arrays.asList(
                Tables.Sounds._ID,
                Tables.Sounds._TYPE,
                Tables.Sounds.TITLE,
                Tables.Sounds.PERMALINK_URL
        ));

        BulkInsertValues.Builder policyBuilder = new BulkInsertValues.Builder(Arrays.asList(
                Tables.TrackPolicies.TRACK_ID,
                Tables.TrackPolicies.MONETIZATION_MODEL
        ));

        for (int i = 1; i <= BATCH_TRACKS_COUNT; i++) {
            trackBuilder.addRow(Arrays.asList(
               i,
               TYPE_TRACK,
               "Track #" + i,
               "track-" + i
            ));

            policyBuilder.addRow(Arrays.asList(
                    i,
                    Strings.EMPTY
            ));
            trackUrns.add(Urn.forTrack(i));
        }

        propeller().bulkInsert(Tables.Sounds.TABLE, trackBuilder.build());
        propeller().bulkInsert(Tables.TrackPolicies.TABLE, policyBuilder.build());

        return trackUrns;
    }

}
