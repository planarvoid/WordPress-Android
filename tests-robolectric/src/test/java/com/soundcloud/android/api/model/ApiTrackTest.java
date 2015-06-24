package com.soundcloud.android.api.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class ApiTrackTest {

    @Test
    public void shouldDeserialize() throws Exception {
        ApiTrack apiTrack = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("suggested_track.json"), ApiTrack.class);

        expect(apiTrack.getUrn().toString()).toEqual("soundcloud:tracks:105834033");
        expect(apiTrack.getTitle()).toEqual("[Sketch] - Beloved");
        expect(apiTrack.getGenre()).toEqual("Piano");
        expect(apiTrack.getUser().getUsername()).toEqual("georgegao");
        expect(apiTrack.getUser().getAvatarUrl()).toEqual("http://i1.sndcdn.com/avatars-000018614344-2p78eh-large.jpg?f34f187");
        expect(apiTrack.getUser().getUrn().toString()).toEqual("soundcloud:users:106815");
        expect(apiTrack.isCommentable()).toBeTrue();
        expect(apiTrack.getStreamUrl()).toEqual("http://media.soundcloud.com/stream/whVhoRw2gpUh");
        expect(apiTrack.getWaveformUrl()).toEqual("https://wis.sndcdn.com/whVhoRw2gpUh.png");
        expect(apiTrack.getArtworkUrl()).toEqual("http://i1.sndcdn.com/artworks-000056989650-zm98k6-large.jpg?5e64f12");
        expect(apiTrack.getUserTags()).toContainExactly("Jazz","Film");
        expect(apiTrack.getCreatedAt()).toEqual(PublicApiWrapper.CloudDateFormat.fromString("2013/08/17 07:50:03 +0000"));
        expect(apiTrack.getSharing()).toBe(Sharing.PRIVATE);
        expect(apiTrack.getStats().getPlaybackCount()).toEqual(4901);
        expect(apiTrack.getPermalinkUrl()).toEqual("http://soundcloud.com/asdffdsa");
        expect(apiTrack.isMonetizable()).toBeTrue();
        expect(apiTrack.getPolicy()).toEqual("monetizable");
    }

    @Test
    public void shouldBeParcelable() throws Exception {
        ApiTrack apiTrack = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("suggested_track.json"), ApiTrack.class);

        Parcel parcel = Parcel.obtain();
        apiTrack.writeToParcel(parcel, 0);

        compareSuggestedTracks(apiTrack, new ApiTrack(parcel));
    }

    @Test
    public void shouldConvertToPropertySet() {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);

        PropertySet propertySet = track.toPropertySet();

        expect(propertySet.get(TrackProperty.URN)).toEqual(track.getUrn());
        expect(propertySet.get(TrackProperty.TITLE)).toEqual(track.getTitle());
        expect(propertySet.get(TrackProperty.CREATED_AT)).toEqual(track.getCreatedAt());
        expect(propertySet.get(TrackProperty.DURATION)).toEqual(track.getDuration());
        expect(propertySet.get(TrackProperty.IS_PRIVATE)).toEqual(track.isPrivate());
        expect(propertySet.get(TrackProperty.WAVEFORM_URL)).toEqual(track.getWaveformUrl());
        expect(propertySet.get(TrackProperty.PERMALINK_URL)).toEqual(track.getPermalinkUrl());
        expect(propertySet.get(TrackProperty.MONETIZABLE)).toEqual(track.isMonetizable());
        expect(propertySet.get(TrackProperty.POLICY)).toEqual(track.getPolicy());
        expect(propertySet.get(TrackProperty.PLAY_COUNT)).toEqual(track.getStats().getPlaybackCount());
        expect(propertySet.get(TrackProperty.COMMENTS_COUNT)).toEqual(track.getStats().getCommentsCount());
        expect(propertySet.get(TrackProperty.LIKES_COUNT)).toEqual(track.getStats().getLikesCount());
        expect(propertySet.get(TrackProperty.REPOSTS_COUNT)).toEqual(track.getStats().getRepostsCount());
        expect(propertySet.get(TrackProperty.SUB_MID_TIER)).toEqual(track.isSubMidTier().get());
        expect(propertySet.get(TrackProperty.MONETIZATION_MODEL)).toEqual(track.getMonetizationModel().get());

        expect(propertySet.get(TrackProperty.CREATOR_NAME)).toEqual(track.getUserName());
        expect(propertySet.get(TrackProperty.CREATOR_URN)).toEqual(track.getUser().getUrn());
    }

    private void compareSuggestedTracks(ApiTrack track1, ApiTrack track2){
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
        expect(track1.isMonetizable()).toEqual(track2.isMonetizable());
        expect(track1.getPolicy()).toEqual(track2.getPolicy());
    }

}
