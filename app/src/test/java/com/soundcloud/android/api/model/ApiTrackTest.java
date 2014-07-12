package com.soundcloud.android.api.model;

import com.soundcloud.android.Expect;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class ApiTrackTest {

    @Test
    public void shouldDeserialize() throws Exception {
        ApiTrack apiTrack = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("suggested_track.json"), ApiTrack.class);


        Expect.expect(apiTrack.getUrn().toString()).toEqual("soundcloud:sounds:105834033");
        Expect.expect(apiTrack.getTitle()).toEqual("[Sketch] - Beloved");
        Expect.expect(apiTrack.getGenre()).toEqual("Piano");
        Expect.expect(apiTrack.getUser().getUsername()).toEqual("georgegao");
        Expect.expect(apiTrack.getUser().getAvatarUrl()).toEqual("http://i1.sndcdn.com/avatars-000018614344-2p78eh-large.jpg?f34f187");
        Expect.expect(apiTrack.getUser().getUrn().toString()).toEqual("soundcloud:users:106815");
        Expect.expect(apiTrack.isCommentable()).toBeTrue();
        Expect.expect(apiTrack.getStreamUrl()).toEqual("http://media.soundcloud.com/stream/whVhoRw2gpUh");
        Expect.expect(apiTrack.getWaveformUrl()).toEqual("https://wis.sndcdn.com/whVhoRw2gpUh.png");
        Expect.expect(apiTrack.getArtworkUrl()).toEqual("http://i1.sndcdn.com/artworks-000056989650-zm98k6-large.jpg?5e64f12");
        Expect.expect(apiTrack.getUserTags()).toContainExactly("Jazz","Film");
        Expect.expect(apiTrack.getCreatedAt()).toEqual(PublicApiWrapper.CloudDateFormat.fromString("2013/08/17 07:50:03 +0000"));
        Expect.expect(apiTrack.getSharing()).toBe(Sharing.PRIVATE);
        Expect.expect(apiTrack.getStats().getPlaybackCount()).toEqual(4901);
        Expect.expect(apiTrack.getPermalinkUrl()).toEqual("http://soundcloud.com/asdffdsa");
        Expect.expect(apiTrack.isMonetizable()).toBeTrue();

    }

    @Test
    public void shouldBeParcelable() throws Exception {
        ApiTrack apiTrack = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("suggested_track.json"), ApiTrack.class);

        Parcel parcel = Parcel.obtain();
        apiTrack.writeToParcel(parcel, 0);

      compareSuggestedTracks(apiTrack, new ApiTrack(parcel));
    }

    private void compareSuggestedTracks(ApiTrack track1, ApiTrack track2){
        Expect.expect(track1.getId()).toEqual(track2.getId());
        Expect.expect(track1.getUrn()).toEqual(track2.getUrn());
        Expect.expect(track1.getTitle()).toEqual(track2.getTitle());
        Expect.expect(track1.getGenre()).toEqual(track2.getGenre());
        Expect.expect(track1.getUser()).toEqual(track2.getUser());
        Expect.expect(track1.isCommentable()).toEqual(track2.isCommentable());
        Expect.expect(track1.getStreamUrl()).toEqual(track2.getStreamUrl());
        Expect.expect(track1.getArtworkUrl()).toEqual(track2.getArtworkUrl());
        Expect.expect(track1.getWaveformUrl()).toEqual(track2.getWaveformUrl());
        Expect.expect(track1.getUserTags()).toEqual(track2.getUserTags());
        Expect.expect(track1.getCreatedAt()).toEqual(track2.getCreatedAt());
        Expect.expect(track1.getPermalinkUrl()).toEqual(track2.getPermalinkUrl());
        Expect.expect(track1.isMonetizable()).toEqual(track2.isMonetizable());
    }

}
