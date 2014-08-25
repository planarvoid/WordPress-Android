package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

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
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.FOLLOW);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("user_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromToggleToUnfollow() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleFollow(false, "screen", 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.UNFOLLOW);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("user_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromLikedTrack() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleLike(true, "screen", new PublicApiTrack(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.LIKE);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("track");
        expect(uiEventAttributes.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromLikedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleLike(true, "screen", new PublicApiPlaylist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.LIKE);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("playlist");
        expect(uiEventAttributes.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromUnlikedTrack() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleLike(false, "screen", new PublicApiTrack(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.UNLIKE);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("track");
        expect(uiEventAttributes.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromUnlikedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleLike(false, "screen", new PublicApiPlaylist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.UNLIKE);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("playlist");
        expect(uiEventAttributes.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromRepostedTrack() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, "screen", new PublicApiTrack(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.REPOST);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("track");
        expect(uiEventAttributes.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromRepostedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, "screen", new PublicApiPlaylist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.REPOST);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("playlist");
        expect(uiEventAttributes.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromUnrepostedTrack() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, "screen", new PublicApiTrack(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.UNREPOST);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("track");
        expect(uiEventAttributes.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromUnrepostedPlaylist() throws Exception {
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, "screen", new PublicApiPlaylist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.UNREPOST);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("playlist");
        expect(uiEventAttributes.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistIsNew() throws Exception {
        UIEvent uiEvent = UIEvent.fromAddToPlaylist("screen", true, 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.ADD_TO_PLAYLIST);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("is_new_playlist")).toEqual("yes");
        expect(uiEventAttributes.get("track_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistExisted() throws Exception {
        UIEvent uiEvent = UIEvent.fromAddToPlaylist("screen", false, 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.ADD_TO_PLAYLIST);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("is_new_playlist")).toEqual("no");
        expect(uiEventAttributes.get("track_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromComment() throws Exception {
        UIEvent uiEvent = UIEvent.fromComment("screen", 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.COMMENT);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("track_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromTrackShare() throws Exception {
        UIEvent uiEvent = UIEvent.fromShare("screen", new PublicApiTrack(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.SHARE);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("track");
        expect(uiEventAttributes.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromPlaylistShare() throws Exception {
        UIEvent uiEvent = UIEvent.fromShare("screen", new PublicApiPlaylist(30));
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.SHARE);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("playlist");
        expect(uiEventAttributes.get("resource_id")).toEqual("30");
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
        PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123));
        UIEvent uiEvent = UIEvent.fromAudioAdCompanionDisplayClick(audioAd, Urn.forTrack(456), 1000L);
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.AUDIO_AD_CLICK);
        expect(uiEvent.getTimestamp()).toEqual(1000L);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEventAttributes.get("ad_urn")).toEqual(audioAd.get(AdProperty.AD_URN));
        expect(uiEventAttributes.get("ad_monetized_urn")).toEqual(Urn.forTrack(123).toString());
        expect(uiEventAttributes.get("ad_track_urn")).toEqual(Urn.forTrack(456).toString());
        expect(uiEventAttributes.get("ad_click_url")).toEqual(audioAd.get(AdProperty.CLICK_THROUGH_LINK).toString());
        expect(uiEventAttributes.get("ad_image_url")).toEqual(audioAd.get(AdProperty.ARTWORK).toString());
        expect(uiEvent.getAudioAdClickthroughUrls()).toContain("click1", "click2");
    }

    @Test
    public void shouldCreateEventFromSkipAudioAdClick() {
        PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123));
        UIEvent uiEvent = UIEvent.fromSkipAudioAdClick(audioAd, Urn.forTrack(456), 1000L);
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.SKIP_AUDIO_AD_CLICK);
        expect(uiEvent.getTimestamp()).toEqual(1000L);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEventAttributes.get("ad_urn")).toEqual(audioAd.get(AdProperty.AD_URN));
        expect(uiEventAttributes.get("ad_monetized_urn")).toEqual(Urn.forTrack(123).toString());
        expect(uiEventAttributes.get("ad_track_urn")).toEqual(Urn.forTrack(456).toString());
        expect(uiEventAttributes.get("ad_image_url")).toEqual(audioAd.get(AdProperty.ARTWORK).toString());
        expect(uiEvent.getAudioAdSkipUrls()).toContain("skip1", "skip2");
    }

}
