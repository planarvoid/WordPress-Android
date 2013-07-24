package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

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

    @Test
    public void shouldBeParcelable() throws Exception {
        SuggestedTrack suggestedTrack = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("suggested_track.json"), SuggestedTrack.class);

        Parcel parcel = Parcel.obtain();
        suggestedTrack.writeToParcel(parcel, 0);

      compareSuggestedTracks(suggestedTrack,new SuggestedTrack(parcel) );
    }

    private void compareSuggestedTracks(SuggestedTrack track1, SuggestedTrack track2){
        expect(track1.getId()).toEqual(track2.getId());
        expect(track1.getUrn()).toEqual(track2.getUrn());
        expect(track1.getTitle()).toEqual(track2.getTitle());
        expect(track1.getGenre()).toEqual(track2.getGenre());
        expect(track1.getUser()).toEqual(track2.getUser());
        expect(track1.isCommentable()).toEqual(track2.isCommentable());
        expect(track1.getStreamUrl()).toEqual(track2.getStreamUrl());
        expect(track1.getmWaveformUrl()).toEqual(track2.getmWaveformUrl());
        expect(track1.getTagList()).toEqual(track2.getTagList());
        expect(track1.getCreatedAt()).toEqual(track2.getCreatedAt());
    }

}
