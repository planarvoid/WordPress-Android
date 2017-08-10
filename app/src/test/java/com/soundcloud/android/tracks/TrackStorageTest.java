package com.soundcloud.android.tracks;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class TrackStorageTest extends StorageIntegrationTest {

    private static final int BATCH_TRACKS_COUNT = 1500;
    private TrackStorage storage;

    @Before
    public void setup() {
        storage = new TrackStorage(propellerRxV2());
    }

    @Test
    public void loadsTrack() {
        ApiTrack apiTrack = testFixtures().insertTrack();

        Track track = storage.loadTrack(apiTrack.getUrn()).blockingGet();

        assertThat(track).isEqualTo(Track.from(apiTrack));
    }

    @Test
    public void loadsDownloadedTrack() {
        ApiTrack apiTrack = testFixtures().insertTrack();
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), 0, 1000L);

        Track track = storage.loadTrack(apiTrack.getUrn()).blockingGet();

        final Track.Builder expected = Track.from(apiTrack).toBuilder();
        expected.offlineState(OfflineState.DOWNLOADED);
        assertThat(track).isEqualTo(expected.build());
    }

    @Test
    public void loadsPendingRemovalTrack() {
        ApiTrack apiTrack = testFixtures().insertTrack();
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 2000L);

        Track track = storage.loadTrack(apiTrack.getUrn()).blockingGet();

        final Track.Builder expected = Track.from(apiTrack).toBuilder();
        expected.offlineState(OfflineState.NOT_OFFLINE);
        assertThat(track).isEqualTo(expected.build());
    }

    @Test
    public void loadsBlockedTrack() {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setBlocked(true);
        testFixtures().insertTrack(apiTrack);

        Track track = storage.loadTrack(apiTrack.getUrn()).blockingGet();

        assertThat(track).isEqualTo(Track.from(apiTrack));
    }

    @Test
    public void loadsSnippedTrack() {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setSnipped(true);
        apiTrack.setSyncable(false);
        apiTrack.setGenre(null);
        testFixtures().insertTrack(apiTrack);

        Track track = storage.loadTrack(apiTrack.getUrn()).blockingGet();

        assertThat(track).isEqualTo(Track.from(apiTrack));
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

        Track track = storage.loadTrack(apiTrack.getUrn()).blockingGet();

        assertThat(track.userLike()).isTrue();
    }

    @Test
    public void loadUnlikedTrack() {
        ApiTrack apiTrack = testFixtures().insertTrack();

        Track track = storage.loadTrack(apiTrack.getUrn()).blockingGet();

        assertThat(track.userLike()).isFalse();
    }

    @Test
    public void shouldReturnEmptyItemIfTrackNotFound() {
        final TestObserver<Track> observer = storage.loadTrack(Urn.forTrack(123)).test();

        observer.assertNoValues();
    }

    @Test
    public void descriptionByUrnEmitsInsertedDescription() {
        ApiTrack track = testFixtures().insertTrack();
        testFixtures().insertDescription(track.getUrn(), "description123");

        Optional<String> description = storage.loadTrackDescription(track.getUrn()).blockingGet();

        assertThat(description.get()).isEqualTo("description123");
    }

    @Test
    public void availableTracksReturnsEmptyWhenStorageIsEmpty() {
        final TestObserver<List<Urn>> subscriber = storage.availableTracks(singletonList(Urn.forTrack(133L))).test();

        subscriber.assertValue(Collections.emptyList());
    }

    @Test
    public void availableTracksReturnsEmptyWhenNoneIsPresent() {
        testFixtures().insertTrack();

        final TestObserver<List<Urn>> subscriber = storage.availableTracks(singletonList(Urn.forTrack(731982L))).test();

        subscriber.assertValue(Collections.emptyList());
    }

    @Test
    public void availableTracksReturnsPresentTracks() {
        ApiTrack apiTrack = testFixtures().insertTrack();

        final TestObserver<List<Urn>> subscriber = storage.availableTracks(asList(apiTrack.getUrn(), Urn.forTrack(731982L))).test();

        subscriber.assertValue(singletonList(apiTrack.getUrn()));
    }

    @Test
    public void loadTracksSetsUserLikeIndividually() {
        ApiTrack likedApiTrack = testFixtures().insertLikedTrack(new Date());
        ApiTrack apiTrack = testFixtures().insertTrack();

        final TestObserver<Map<Urn, Track>> subscriber = storage.loadTracks(asList(likedApiTrack.getUrn(), apiTrack.getUrn())).test();
        Map<Urn, Track> map = subscriber.values().get(0);

        assertThat(map.get(likedApiTrack.getUrn()).userLike()).isTrue();
        assertThat(map.get(apiTrack.getUrn()).userLike()).isFalse();
    }

    @Test
    public void loadTracksSetsUserRepostsIndividually() {
        ApiTrack postedApiTrack = testFixtures().insertPostedTrack(new Date(), true);
        ApiTrack apiTrack = testFixtures().insertTrack();

        final TestObserver<Map<Urn, Track>> subscriber = storage.loadTracks(asList(postedApiTrack.getUrn(), apiTrack.getUrn())).test();
        Map<Urn, Track> map = subscriber.values().get(0);

        assertThat(map.get(postedApiTrack.getUrn()).userRepost()).isTrue();
        assertThat(map.get(apiTrack.getUrn()).userRepost()).isFalse();
    }

    @Test
    public void loadTracksInBatches() {
        List<Urn> trackUrns = testFixtures().insertTracks(BATCH_TRACKS_COUNT);

        final TestObserver<Map<Urn, Track>> subscriber = storage.loadTracks(trackUrns).test();

        assertThat(subscriber.values().get(0).keySet()).isEqualTo(new HashSet<>(trackUrns));
    }

    @Test
    public void loadAvailableTracksInBatches() {
        List<Urn> trackUrns = testFixtures().insertTracks(BATCH_TRACKS_COUNT);

        final TestObserver<List<Urn>> subscriber = storage.availableTracks(trackUrns).test();

        assertThat(subscriber.values().get(0)).isEqualTo(trackUrns);
    }

    @Test
    public void loadsUrnByPermalink() throws Exception {
        List<Urn> trackUrns = testFixtures().insertTracks(BATCH_TRACKS_COUNT);
        Urn expectedUrn = trackUrns.get(666);
        String permalinkUrl = storage.loadTrack(expectedUrn).blockingGet().permalinkUrl();
        String permalink = permalinkUrl.replace("https://soundcloud.com/", "");

        final Urn urn = storage.urnForPermalink(permalink).blockingGet();

        assertThat(urn).isEqualTo(expectedUrn);
    }

    @Test
    public void loadsUrnByPermalinkNotFound() throws Exception {
        testFixtures().insertTracks(BATCH_TRACKS_COUNT);

        storage.urnForPermalink("testing")
               .test()
               .assertNoValues()
               .assertComplete();
    }
}
