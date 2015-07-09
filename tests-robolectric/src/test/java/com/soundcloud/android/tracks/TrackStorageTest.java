package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class TrackStorageTest extends StorageIntegrationTest {

    private TrackStorage storage;

    @Before
    public void setup() {
        storage = new TrackStorage(propellerRx());
    }

    @Test
    public void loadsTrack() throws Exception {
        ApiTrack apiTrack = testFixtures().insertTrack();

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        expect(track).toEqual(TestPropertySets.fromApiTrack(apiTrack));
    }

    @Test
    public void loadsDownloadedTrack() throws Exception {
        ApiTrack apiTrack = testFixtures().insertTrack();
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), 0, 1000L);

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        final PropertySet expected = TestPropertySets.fromApiTrack(apiTrack);
        expected.put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADED);
        expect(track).toEqual(expected);
    }

    @Test
    public void loadsPendingRemovalTrack() throws Exception {
        ApiTrack apiTrack = testFixtures().insertTrack();
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 2000L);

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        final PropertySet expected = TestPropertySets.fromApiTrack(apiTrack);
        expected.put(OfflineProperty.OFFLINE_STATE, OfflineState.NO_OFFLINE);
        expect(track).toEqual(expected);
    }

    @Test
    public void doesntCrashOnNullWaveforms() throws Exception {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setWaveformUrl(null);
        testFixtures().insertTrack(apiTrack);

        storage.loadTrack(apiTrack.getUrn());
    }

    @Test
    public void loadLikedTrack() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date());

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        expect(track.get(PlayableProperty.IS_LIKED)).toBeTrue();
    }

    @Test
    public void loadUnlikedTrack() throws Exception {
        ApiTrack apiTrack = testFixtures().insertTrack();

        PropertySet track = storage.loadTrack(apiTrack.getUrn()).toBlocking().single();

        expect(track.get(PlayableProperty.IS_LIKED)).toBeFalse();
    }

    @Test
    public void shouldReturnEmptyPropertySetIfTrackNotFound() throws Exception {
        PropertySet track = storage.loadTrack(Urn.forTrack(123)).toBlocking().single();

        expect(track).toEqual(PropertySet.create());
    }

    @Test
    public void descriptionByUrnEmitsInsertedDescription() throws Exception {
        final Urn trackUrn = Urn.forTrack(123);
        testFixtures().insertDescription(trackUrn, "description123");

        PropertySet description = storage.loadTrackDescription(trackUrn).toBlocking().single();

        expect(description).toEqual(PropertySet.from(TrackProperty.DESCRIPTION.bind("description123")));
    }
}