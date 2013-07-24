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
        expect(suggestedTrack.getTitle()).toEqual("Evolution of Get Lucky [Daft Punk chronologic cover]");
        expect(suggestedTrack.getGenre()).toEqual("all genres");
        expect(suggestedTrack.getUser().getUsername()).toEqual("pvnova");
        expect(suggestedTrack.getUser().getUrn()).toEqual(ClientUri.fromUri("soundcloud:users:7861090"));
        expect(suggestedTrack.isCommentable()).toBeTrue();
        expect(suggestedTrack.getStreamUrl()).toEqual("https://api.soundcloud.com/tracks/96017719/stream");
        expect(suggestedTrack.getmWaveformUrl()).toEqual("https://w1.sndcdn.com/GXIXy4KWvMRG_m.png");
        expect(suggestedTrack.getTagList()).toContainExactly("daft","punk","cover","chronologic");
        expect(suggestedTrack.getCreatedAt().toString()).toEqual("Sat Jun 08 18:59:05 CEST 2013");
    }
}
