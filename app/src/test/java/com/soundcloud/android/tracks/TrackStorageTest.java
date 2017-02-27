package com.soundcloud.android.tracks;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

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

        Optional<Track> track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        assertThat(track.get()).isEqualTo(Track.from(apiTrack));
    }

    @Test
    public void loadsDownloadedTrack() {
        ApiTrack apiTrack = testFixtures().insertTrack();
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), 0, 1000L);

        Optional<Track> track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        final Track.Builder expected = Track.builder(Track.from(apiTrack));
        expected.offlineState(OfflineState.DOWNLOADED);
        assertThat(track.get()).isEqualTo(expected.build());
    }

    @Test
    public void loadsPendingRemovalTrack() {
        ApiTrack apiTrack = testFixtures().insertTrack();
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 2000L);

        Optional<Track> track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        final Track.Builder expected = Track.builder(Track.from(apiTrack));
        expected.offlineState(OfflineState.NOT_OFFLINE);
        assertThat(track.get()).isEqualTo(expected.build());
    }

    @Test
    public void loadsBlockedTrack() {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setBlocked(true);
        testFixtures().insertTrack(apiTrack);

        Optional<Track> track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        assertThat(track.get()).isEqualTo(Track.from(apiTrack));
    }

    @Test
    public void loadsSnippedTrack() {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setSnipped(true);
        apiTrack.setSyncable(false);
        apiTrack.setGenre(null);
        testFixtures().insertTrack(apiTrack);

        Optional<Track> track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        assertThat(track.get()).isEqualTo(Track.from(apiTrack));
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

        Optional<Track> track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        assertThat(track.get().userLike()).isTrue();
    }

    @Test
    public void loadUnlikedTrack() {
        ApiTrack apiTrack = testFixtures().insertTrack();

        Optional<Track> track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        assertThat(track.get().userLike()).isFalse();
    }

    @Test
    public void shouldReturnEmptyItemIfTrackNotFound() {
        Optional<Track> track = storage.loadTrack(Urn.forTrack(123)).toBlocking().single();

        assertThat(track.isPresent()).isFalse();
    }

    @Test
    public void descriptionByUrnEmitsInsertedDescription() {
        ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertDescription(track.getUrn(), "description123");

        Optional<String> description = storage.loadTrackDescription(track.getUrn()).toBlocking().single();

        assertThat(description.get()).isEqualTo("description123");
    }

    @Test
    public void availableTracksReturnsEmptyWhenStorageIsEmpty() {
        final TestSubscriber<List<Urn>> subscriber = new TestSubscriber<>();

        storage.availableTracks(singletonList(Urn.forTrack(133L))).subscribe(subscriber);

        subscriber.assertValue(Collections.emptyList());
    }

    @Test
    public void availableTracksReturnsEmptyWhenNoneIsPresent() {
        final TestSubscriber<List<Urn>> subscriber = new TestSubscriber<>();
        testFixtures().insertTrack();

        storage.availableTracks(singletonList(Urn.forTrack(731982L))).subscribe(subscriber);

        subscriber.assertValue(Collections.emptyList());
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
        final TestSubscriber<Map<Urn, Track>> subscriber = new TestSubscriber<>();
        ApiTrack likedApiTrack = testFixtures().insertLikedTrack(new Date());
        ApiTrack apiTrack = testFixtures().insertTrack();

        storage.loadTracks(asList(likedApiTrack.getUrn(), apiTrack.getUrn())).subscribe(subscriber);
        Map<Urn, Track> map = subscriber.getOnNextEvents().get(0);

        assertThat(map.get(likedApiTrack.getUrn()).userLike()).isTrue();
        assertThat(map.get(apiTrack.getUrn()).userLike()).isFalse();
    }

    @Test
    public void loadTracksSetsUserRepostsIndividually() {
        final TestSubscriber<Map<Urn, Track>> subscriber = new TestSubscriber<>();
        ApiTrack postedApiTrack = testFixtures().insertPostedTrack(new Date(), true);
        ApiTrack apiTrack = testFixtures().insertTrack();

        storage.loadTracks(asList(postedApiTrack.getUrn(), apiTrack.getUrn())).subscribe(subscriber);
        Map<Urn, Track> map = subscriber.getOnNextEvents().get(0);

        assertThat(map.get(postedApiTrack.getUrn()).userRepost()).isTrue();
        assertThat(map.get(apiTrack.getUrn()).userRepost()).isFalse();
    }

    @Test
    public void loadTracksInBatches() {
        final TestSubscriber<Map<Urn, Track>> subscriber = new TestSubscriber<>();
        List<Urn> trackUrns = testFixtures().insertTracks(BATCH_TRACKS_COUNT);

        storage.loadTracks(trackUrns).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents().get(0).keySet()).isEqualTo(new HashSet<>(trackUrns));
    }

    @Test
    public void loadAvailableTracksInBatches() {
        final TestSubscriber<List<Urn>> subscriber = new TestSubscriber<>();
        List<Urn> trackUrns = testFixtures().insertTracks(BATCH_TRACKS_COUNT);

        storage.availableTracks(trackUrns).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(trackUrns);
    }
}
