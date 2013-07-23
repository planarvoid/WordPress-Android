package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedTrackTest {

    @Test
    public void shouldDeserialize() throws Exception {
        SuggestedTrack suggestedTrack = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("suggested_track.json"), SuggestedTrack.class);
        expect(suggestedTrack.getUrn()).toEqual(ClientUri.fromUri("soundcloud:sounds:96017719"));
    }
}
