package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;
import static org.junit.Assert.assertEquals;

import com.soundcloud.android.TestPropertySets;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class UIEventTest {

    @Test
    public void shouldCreateEventFromToggleToFollow() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleFollow(true, "screen", 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), UIEvent.Kind.FOLLOW);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("user_id"), "30");
    }

    @Test
    public void shouldCreateEventFromToggleToUnfollow() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleFollow(false, "screen", 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), UIEvent.Kind.UNFOLLOW);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("user_id"), "30");
    }

    @Test
    public void shouldCreateEventFromLikedTrack() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleLike(true, "screen", new PublicApiTrack(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), UIEvent.Kind.LIKE);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "track");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromLikedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleLike(true, "screen", new PublicApiPlaylist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), UIEvent.Kind.LIKE);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "playlist");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnlikedTrack() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleLike(false, "screen", new PublicApiTrack(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), UIEvent.Kind.UNLIKE);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "track");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnlikedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleLike(false, "screen", new PublicApiPlaylist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), UIEvent.Kind.UNLIKE);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "playlist");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromRepostedTrack() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, "screen", new PublicApiTrack(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), UIEvent.Kind.REPOST);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "track");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromRepostedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, "screen", new PublicApiPlaylist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), UIEvent.Kind.REPOST);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "playlist");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnrepostedTrack() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, "screen", new PublicApiTrack(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), UIEvent.Kind.UNREPOST);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "track");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromUnrepostedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, "screen", new PublicApiPlaylist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), UIEvent.Kind.UNREPOST);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "playlist");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistIsNew() throws Exception {
        UIEvent uiEvent = UIEvent.fromAddToPlaylist("screen", true, 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), UIEvent.Kind.ADD_TO_PLAYLIST);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("is_new_playlist"), "yes");
        assertEquals(uiEventAttributes.get("track_id"), "30");
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistExisted() throws Exception {
        UIEvent uiEvent = UIEvent.fromAddToPlaylist("screen", false, 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), UIEvent.Kind.ADD_TO_PLAYLIST);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("is_new_playlist"), "no");
        assertEquals(uiEventAttributes.get("track_id"), "30");
    }

    @Test
    public void shouldCreateEventFromComment() throws Exception {
        UIEvent uiEvent = UIEvent.fromComment("screen", 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), UIEvent.Kind.COMMENT);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("track_id"), "30");
    }

    @Test
    public void shouldCreateEventFromTrackShare() throws Exception {
        UIEvent uiEvent = UIEvent.fromShare("screen", new PublicApiTrack(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), UIEvent.Kind.SHARE);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "track");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromPlaylistShare() throws Exception {
        UIEvent uiEvent = UIEvent.fromShare("screen", new PublicApiPlaylist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        assertEquals(uiEvent.getKind(), UIEvent.Kind.SHARE);
        assertEquals(uiEventAttributes.get("context"), "screen");
        assertEquals(uiEventAttributes.get("resource"), "playlist");
        assertEquals(uiEventAttributes.get("resource_id"), "30");
    }

    @Test
    public void shouldCreateEventFromShuffleMyLikes() throws Exception {
        expect(UIEvent.fromShuffleMyLikes().getKind()).toEqual(UIEvent.Kind.SHUFFLE_LIKES);
    }

    @Test
    public void shouldCreateEventFromProfileNavigation() throws Exception {
        UIEvent uiEvent = UIEvent.fromProfileNav();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.NAVIGATION);
        expect(uiEventAttributes.get("page")).toEqual("you");
    }

    @Test
    public void shouldCreateEventFromStreamNavigation() throws Exception {
        UIEvent uiEvent = UIEvent.fromStreamNav();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.NAVIGATION);
        expect(uiEventAttributes.get("page")).toEqual("stream");
    }

    @Test
    public void shouldCreateEventFromExploreNavigation() throws Exception {
        UIEvent uiEvent = UIEvent.fromExploreNav();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.NAVIGATION);
        expect(uiEventAttributes.get("page")).toEqual("explore");
    }

    @Test
    public void shouldCreateEventFromLikesNavigation() throws Exception {
        UIEvent uiEvent = UIEvent.fromLikesNav();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.NAVIGATION);
        expect(uiEventAttributes.get("page")).toEqual("collection_likes");
    }

    @Test
    public void shouldCreateEventFromPlaylistsNavigation() throws Exception {
        UIEvent uiEvent = UIEvent.fromPlaylistsNav();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.NAVIGATION);
        expect(uiEventAttributes.get("page")).toEqual("collection_playlists");
    }

    @Test
    public void shouldCreateEventFromSearchNavigation() throws Exception {
        UIEvent uiEvent = UIEvent.fromSearchAction();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.NAVIGATION);
        expect(uiEventAttributes.get("page")).toEqual("search");
    }

    @Test
    public void shouldCreateEventFromAudioAdClick() {
        PropertySet audioAd = TestPropertySets.expectedAudioAdForAnalytics(Urn.forTrack(123));
        UIEvent uiEvent = UIEvent.fromAudioAdCompanionDisplayClick(audioAd, Urn.forTrack(456), 1000L);
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.AUDIO_AD_CLICK);
        expect(uiEvent.getTimestamp()).toEqual(1000L);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEventAttributes.get("ad_urn")).toEqual(audioAd.get(AdProperty.AD_URN));
        expect(uiEventAttributes.get("ad_monetized_urn")).toEqual(Urn.forTrack(123).toString());
        expect(uiEventAttributes.get("ad_track_urn")).toEqual(Urn.forTrack(456).toString());
        expect(uiEventAttributes.get("ad_click_url")).toEqual(audioAd.get(AdProperty.CLICK_THROUGH_LINK).toString());
        expect(uiEventAttributes.get("ad_image_url")).toEqual(audioAd.get(AdProperty.ARTWORK).toString());
    }

    @Test
    public void shouldCreateEventFromSkipAudioAdClick() {
        PropertySet audioAd = TestPropertySets.expectedAudioAdForAnalytics(Urn.forTrack(123));
        UIEvent uiEvent = UIEvent.fromSkipAudioAdClick(audioAd, Urn.forTrack(456), 1000L);
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.SKIP_AUDIO_AD_CLICK);
        expect(uiEvent.getTimestamp()).toEqual(1000L);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEventAttributes.get("ad_urn")).toEqual(audioAd.get(AdProperty.AD_URN));
        expect(uiEventAttributes.get("ad_monetized_urn")).toEqual(Urn.forTrack(123).toString());
        expect(uiEventAttributes.get("ad_track_urn")).toEqual(Urn.forTrack(456).toString());
        expect(uiEventAttributes.get("ad_image_url")).toEqual(audioAd.get(AdProperty.ARTWORK).toString());
    }
}
