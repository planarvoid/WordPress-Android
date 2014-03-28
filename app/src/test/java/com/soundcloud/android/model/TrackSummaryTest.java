package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.http.PublicApiWrapper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class TrackSummaryTest {

    @Test
    public void shouldDeserialize() throws Exception {
        TrackSummary trackSummary = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("suggested_track.json"), TrackSummary.class);


        expect(trackSummary.getUrn()).toEqual("soundcloud:sounds:105834033");
        expect(trackSummary.getTitle()).toEqual("[Sketch] - Beloved");
        expect(trackSummary.getGenre()).toEqual("Piano");
        expect(trackSummary.getUser().getUsername()).toEqual("georgegao");
        expect(trackSummary.getUser().getAvatarUrl()).toEqual("http://i1.sndcdn.com/avatars-000018614344-2p78eh-large.jpg?f34f187");
        expect(trackSummary.getUser().getUrn()).toEqual("soundcloud:users:106815");
        expect(trackSummary.isCommentable()).toBeTrue();
        expect(trackSummary.getStreamUrl()).toEqual("http://media.soundcloud.com/stream/whVhoRw2gpUh");
        expect(trackSummary.getWaveformUrl()).toEqual("https://wis.sndcdn.com/whVhoRw2gpUh.png");
        expect(trackSummary.getArtworkUrl()).toEqual("http://i1.sndcdn.com/artworks-000056989650-zm98k6-large.jpg?5e64f12");
        expect(trackSummary.getUserTags()).toContainExactly("Jazz","Film");
        expect(trackSummary.getCreatedAt()).toEqual(PublicApiWrapper.CloudDateFormat.fromString("2013/08/17 07:50:03 +0000"));
        expect(trackSummary.getSharing()).toBe(Sharing.PRIVATE);
        expect(trackSummary.getStats().getPlaybackCount()).toEqual(4901L);
        expect(trackSummary.getPermalinkUrl()).toEqual("http://soundcloud.com/asdffdsa");

    }

    @Test
    public void shouldBeParcelable() throws Exception {
        TrackSummary trackSummary = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("suggested_track.json"), TrackSummary.class);

        Parcel parcel = Parcel.obtain();
        trackSummary.writeToParcel(parcel, 0);

      compareSuggestedTracks(trackSummary, new TrackSummary(parcel));
    }

    private void compareSuggestedTracks(TrackSummary track1, TrackSummary track2){
        expect(track1.getId()).toEqual(track2.getId());
        expect(track1.getUrn()).toEqual(track2.getUrn());
        expect(track1.getTitle()).toEqual(track2.getTitle());
        expect(track1.getGenre()).toEqual(track2.getGenre());
        expect(track1.getUser()).toEqual(track2.getUser());
        expect(track1.isCommentable()).toEqual(track2.isCommentable());
        expect(track1.getStreamUrl()).toEqual(track2.getStreamUrl());
        expect(track1.getArtworkUrl()).toEqual(track2.getArtworkUrl());
        expect(track1.getWaveformUrl()).toEqual(track2.getWaveformUrl());
        expect(track1.getUserTags()).toEqual(track2.getUserTags());
        expect(track1.getCreatedAt()).toEqual(track2.getCreatedAt());
        expect(track1.getPermalinkUrl()).toEqual(track2.getPermalinkUrl());
    }

}
