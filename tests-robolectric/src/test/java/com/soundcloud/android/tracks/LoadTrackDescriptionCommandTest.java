package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class LoadTrackDescriptionCommandTest extends StorageIntegrationTest {

    private LoadTrackDescriptionCommand command;

    @Before
    public void setup() {
        command = new LoadTrackDescriptionCommand(testScheduler());
    }

    @Test
    public void descriptionByUrnEmitsInsertedDescription() throws Exception {
        final Urn trackUrn = Urn.forTrack(123);
        testFixtures().insertDescription(trackUrn, "description123");

        PropertySet track = command.with(trackUrn).call();

        expect(track).toEqual(PropertySet.from(TrackProperty.DESCRIPTION.bind("description123")));
    }

}