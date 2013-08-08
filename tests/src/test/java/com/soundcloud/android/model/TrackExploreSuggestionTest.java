package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.http.Wrapper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class TrackExploreSuggestionTest {

    @Test
    public void shouldDeserialize() throws Exception {
        TrackExploreSuggestion trackExploreSuggestion = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("suggested_track.json"), TrackExploreSuggestion.class);

        expect(trackExploreSuggestion.getUrn()).toEqual(ClientUri.fromUri("soundcloud:sounds:96017719"));
        expect(trackExploreSuggestion.getTitle()).toEqual("Evolution of Get Lucky [Daft Punk chronologic cover]");
        expect(trackExploreSuggestion.getGenre()).toEqual("all genres");
        expect(trackExploreSuggestion.getUser().getUsername()).toEqual("pvnova");
        expect(trackExploreSuggestion.getUser().getUrn()).toEqual(ClientUri.fromUri("soundcloud:users:7861090"));
        expect(trackExploreSuggestion.isCommentable()).toBeTrue();
        expect(trackExploreSuggestion.getStreamUrl()).toEqual("https://api.soundcloud.com/tracks/96017719/stream");
        expect(trackExploreSuggestion.getmWaveformUrl()).toEqual("https://w1.sndcdn.com/GXIXy4KWvMRG_m.png");
        expect(trackExploreSuggestion.getTagList()).toContainExactly("daft","punk","cover","chronologic");
        expect(trackExploreSuggestion.getCreatedAt()).toEqual(Wrapper.CloudDateFormat.fromString("2013/06/08 16:59:05 +0000"));
    }

    @Test
    public void shouldBeParcelable() throws Exception {
        TrackExploreSuggestion trackExploreSuggestion = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("suggested_track.json"), TrackExploreSuggestion.class);

        Parcel parcel = Parcel.obtain();
        trackExploreSuggestion.writeToParcel(parcel, 0);

      compareSuggestedTracks(trackExploreSuggestion,new TrackExploreSuggestion(parcel) );
    }

    private void compareSuggestedTracks(TrackExploreSuggestion track1, TrackExploreSuggestion track2){
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
