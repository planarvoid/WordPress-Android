package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    public void doesntCrashOnNullWaveforms() throws Exception {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setWaveformUrl(null);
        testFixtures().insertTrack(apiTrack);

        command.with(apiTrack.getUrn()).call();
    }

}