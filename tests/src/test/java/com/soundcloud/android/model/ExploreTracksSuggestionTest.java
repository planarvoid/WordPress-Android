package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.http.Wrapper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.utils.images.ImageSize;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksSuggestionTest {

    @Test
    public void shouldDeserialize() throws Exception {
        ExploreTracksSuggestion exploreTracksSuggestion = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("suggested_track.json"), ExploreTracksSuggestion.class);

        expect(exploreTracksSuggestion.getUrn()).toEqual(ClientUri.fromUri("soundcloud:sounds:105834033"));
        expect(exploreTracksSuggestion.getTitle()).toEqual("[Sketch] - Beloved");
        expect(exploreTracksSuggestion.getGenre()).toEqual("Piano");
        expect(exploreTracksSuggestion.getUser().getUsername()).toEqual("georgegao");
        expect(exploreTracksSuggestion.getUser().getUrn()).toEqual(ClientUri.fromUri("soundcloud:users:106815"));
        expect(exploreTracksSuggestion.isCommentable()).toBeTrue();
        expect(exploreTracksSuggestion.getStreamUrl()).toEqual("http://media.soundcloud.com/stream/whVhoRw2gpUh");
        expect(exploreTracksSuggestion.getWaveformUrl()).toEqual("https://wis.sndcdn.com/whVhoRw2gpUh.png");
        expect(exploreTracksSuggestion.getArtworkUrl()).toEqual("http://i1.sndcdn.com/artworks-000056989650-zm98k6-large.jpg?5e64f12");
        expect(exploreTracksSuggestion.getUserTags()).toContainExactly("Jazz","Film");
        expect(exploreTracksSuggestion.getCreatedAt()).toEqual(Wrapper.CloudDateFormat.fromString("2013/08/17 07:50:03 +0000"));
        expect(exploreTracksSuggestion.getStats().getPlaybackCount()).toEqual(4901L);

    }

    @Test
    public void shouldBeParcelable() throws Exception {
        ExploreTracksSuggestion exploreTracksSuggestion = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("suggested_track.json"), ExploreTracksSuggestion.class);

        Parcel parcel = Parcel.obtain();
        exploreTracksSuggestion.writeToParcel(parcel, 0);

      compareSuggestedTracks(exploreTracksSuggestion, new ExploreTracksSuggestion(parcel));
    }

    @Test
    public void shouldReturnArtworkUrlInCorrectSize(){
        ExploreTracksSuggestion suggestion = new ExploreTracksSuggestion();
        suggestion.setArtworkUrl("http://i1.sndcdn.com/artworks-000056989650-zm98k6-large.jpg?5e64f12");
        expect(suggestion.getArtworkOrAvatar(ImageSize.T500)).toEqual("http://i1.sndcdn.com/artworks-000056989650-zm98k6-t500x500.jpg?5e64f12");
    }

    @Test
    public void shouldReturnAvatarUrlInCorrectSize(){
        ExploreTracksSuggestion suggestion = new ExploreTracksSuggestion();
        suggestion.setUser(new UserSummary("soundcloud:users:123"));
        expect(suggestion.getArtworkOrAvatar(ImageSize.T500)).toEqual("https://api.soundcloud.com/resolve/image?url=soundcloud%3Ausers%3A123&client_id=40ccfee680a844780a41fbe23ea89934&size=t500x500");
    }

    @Test
    public void shouldReturnNullArtworkOrAvatar(){
        ExploreTracksSuggestion suggestion = new ExploreTracksSuggestion();
        expect(suggestion.getArtworkOrAvatar(ImageSize.T500)).toBeNull();
    }

    private void compareSuggestedTracks(ExploreTracksSuggestion track1, ExploreTracksSuggestion track2){
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
    }

}
