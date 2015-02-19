package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PlayableProperty;
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
public class LoadTrackCommandTest extends StorageIntegrationTest {

    private LoadTrackCommand command;

    @Before
    public void setup() {
        command = new LoadTrackCommand(testScheduler());
    }

    @Test
    public void loadsTrack() throws Exception {
        ApiTrack apiTrack = testFixtures().insertTrack();

        PropertySet track = command.with(apiTrack.getUrn()).call();

        expect(track).toEqual(TestPropertySets.fromApiTrack(apiTrack));
    }

    @Test
    public void loadsDownloadedTrack() throws Exception {
        ApiTrack apiTrack = testFixtures().insertTrack();
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), 1000L);

        PropertySet track = command.with(apiTrack.getUrn()).call();

        final PropertySet expected = TestPropertySets.fromApiTrack(apiTrack);
        expected.put(TrackProperty.OFFLINE_DOWNLOADED_AT, new Date(1000L));
        expect(track).toEqual(expected);
    }

    @Test
    public void loadsPendingRemovalTrack() throws Exception {
        ApiTrack apiTrack = testFixtures().insertTrack();
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack.getUrn(), 2000L);

        PropertySet track = command.with(apiTrack.getUrn()).call();

        final PropertySet expected = TestPropertySets.fromApiTrack(apiTrack);
        expected.put(TrackProperty.OFFLINE_REMOVED_AT, new Date(2000L));
        expect(track).toEqual(expected);
    }

    @Test
    public void doesntCrashOnNullWaveforms() throws Exception {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setWaveformUrl(null);
        testFixtures().insertTrack(apiTrack);

        command.with(apiTrack.getUrn()).call();
    }

    @Test
    public void loadLikedTrack() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date());

        PropertySet track = command.with(apiTrack.getUrn()).call();

        expect(track.get(PlayableProperty.IS_LIKED)).toBeTrue();
    }

    @Test
    public void loadUnlikedTrack() throws Exception {
        ApiTrack apiTrack = testFixtures().insertTrack();

        PropertySet track = command.with(apiTrack.getUrn()).call();

        expect(track.get(PlayableProperty.IS_LIKED)).toBeFalse();
    }
}