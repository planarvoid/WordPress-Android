package com.soundcloud.android.tracks;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TrackStorageTest extends StorageIntegrationTest {

    private TrackStorage storage;

    @Before
    public void setup() {
        storage = new TrackStorage(propellerRx());
    }

    @Test
    public void loadsTrack() {
        ApiTrack apiTrack = testFixtures().insertTrack();

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        assertThat(track).isEqualTo(TestPropertySets.fromApiTrack(apiTrack));
    }

    @Test
    public void loadsDownloadedTrack() {
        ApiTrack apiTrack = testFixtures().insertTrack();
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), 0, 1000L);

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        final PropertySet expected = TestPropertySets.fromApiTrack(apiTrack);
        expected.put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADED);
        assertThat(track).isEqualTo(expected);
    }

    @Test
    public void loadsPendingRemovalTrack() {
        ApiTrack apiTrack = testFixtures().insertTrack();
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 2000L);

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        final PropertySet expected = TestPropertySets.fromApiTrack(apiTrack);
        expected.put(OfflineProperty.OFFLINE_STATE, OfflineState.NOT_OFFLINE);
        assertThat(track).isEqualTo(expected);
    }

    @Test
    public void loadsBlockedTrack() {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setBlocked(true);
        testFixtures().insertTrack(apiTrack);

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        assertThat(track).isEqualTo(TestPropertySets.fromApiTrack(apiTrack));
    }

    @Test
    public void loadsSnippedTrack() {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setSnipped(true);
        testFixtures().insertTrack(apiTrack);

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        assertThat(track).isEqualTo(TestPropertySets.fromApiTrack(apiTrack));
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
}
