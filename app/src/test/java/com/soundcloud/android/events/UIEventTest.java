package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.TestPropertySets;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class UIEventTest {

    private static final Urn TRACK_URN = Urn.forTrack(30L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(42L);
    private static final Urn USER_URN = Urn.forUser(2L);

    @Test
    public void shouldCreateEventFromPlayerClose() {
        UIEvent uiEvent = UIEvent.fromPlayerClose("tap_footer");
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();

        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.PLAYER_CLOSE);
        expect(uiEventAttributes.get("method")).toEqual("tap_footer");
    }

    @Test
    public void shouldCreateEventFromPlayerOpen() {
        UIEvent uiEvent = UIEvent.fromPlayerOpen("tap_footer");
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();

        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.PLAYER_OPEN);
        expect(uiEventAttributes.get("method")).toEqual("tap_footer");
    }

    @Test
    public void shouldCreateEventFromToggleToFollow() {
        UIEvent uiEvent = UIEvent.fromToggleFollow(true, "screen", 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.FOLLOW);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("user_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromToggleToUnfollow() {
        UIEvent uiEvent = UIEvent.fromToggleFollow(false, "screen", 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.UNFOLLOW);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("user_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromLikedTrack() {
        UIEvent uiEvent = UIEvent.fromToggleLike(true, "screen", TRACK_URN);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.LIKE);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("track");
        expect(uiEventAttributes.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromLikedPlaylist() {
        UIEvent uiEvent = UIEvent.fromToggleLike(true, "screen", PLAYLIST_URN);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.LIKE);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("playlist");
        expect(uiEventAttributes.get("resource_id")).toEqual("42");
    }

    @Test
    public void shouldCreateEventFromUnlikedTrack() {
        UIEvent uiEvent = UIEvent.fromToggleLike(false, "screen", TRACK_URN);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.UNLIKE);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("track");
        expect(uiEventAttributes.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromUnlikedPlaylist() {
        UIEvent uiEvent = UIEvent.fromToggleLike(false, "screen", PLAYLIST_URN);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.UNLIKE);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("playlist");
        expect(uiEventAttributes.get("resource_id")).toEqual("42");
    }

    @Test
    public void shouldCreateEventWithUnknownResourceForUnexpectedUrnType() {
        UIEvent uiEvent = UIEvent.fromToggleLike(true, "screen", USER_URN);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.LIKE);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("unknown");
        expect(uiEventAttributes.get("resource_id")).toEqual("2");
    }

    @Test
    public void shouldCreateEventFromRepostedTrack() {
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, "screen", TRACK_URN);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.REPOST);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("track");
        expect(uiEventAttributes.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromRepostedPlaylist() {
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, "screen", PLAYLIST_URN);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.REPOST);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("playlist");
        expect(uiEventAttributes.get("resource_id")).toEqual("42");
    }

    @Test
    public void shouldCreateEventFromUnrepostedTrack() {
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, "screen", TRACK_URN);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.UNREPOST);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("track");
        expect(uiEventAttributes.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromUnrepostedPlaylist() {
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, "screen", PLAYLIST_URN);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.UNREPOST);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("playlist");
        expect(uiEventAttributes.get("resource_id")).toEqual("42");
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistIsNew() {
        UIEvent uiEvent = UIEvent.fromAddToPlaylist("screen", true, 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.ADD_TO_PLAYLIST);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("is_new_playlist")).toEqual("yes");
        expect(uiEventAttributes.get("track_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistExisted() {
        UIEvent uiEvent = UIEvent.fromAddToPlaylist("screen", false, 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.ADD_TO_PLAYLIST);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("is_new_playlist")).toEqual("no");
        expect(uiEventAttributes.get("track_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromComment() {
        UIEvent uiEvent = UIEvent.fromComment("screen", 30);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.COMMENT);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("track_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromTrackShare() {
        UIEvent uiEvent = UIEvent.fromShare("screen", TRACK_URN);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.SHARE);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("track");
        expect(uiEventAttributes.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromPlaylistShare() {
        UIEvent uiEvent = UIEvent.fromShare("screen", PLAYLIST_URN);
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.SHARE);
        expect(uiEventAttributes.get("context")).toEqual("screen");
        expect(uiEventAttributes.get("resource")).toEqual("playlist");
        expect(uiEventAttributes.get("resource_id")).toEqual("42");
    }

    @Test
    public void shouldCreateEventFromShuffleMyLikes() {
        expect(UIEvent.fromShuffleMyLikes().getKind()).toEqual(UIEvent.Kind.SHUFFLE_LIKES);
    }

    @Test
    public void shouldCreateEventFromProfileNavigation() {
        UIEvent uiEvent = UIEvent.fromProfileNav();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.NAVIGATION);
        expect(uiEventAttributes.get("page")).toEqual("you");
    }

    @Test
    public void shouldCreateEventFromStreamNavigation() {
        UIEvent uiEvent = UIEvent.fromStreamNav();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.NAVIGATION);
        expect(uiEventAttributes.get("page")).toEqual("stream");
    }

    @Test
    public void shouldCreateEventFromExploreNavigation() {
        UIEvent uiEvent = UIEvent.fromExploreNav();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.NAVIGATION);
        expect(uiEventAttributes.get("page")).toEqual("explore");
    }

    @Test
    public void shouldCreateEventFromLikesNavigation() {
        UIEvent uiEvent = UIEvent.fromLikesNav();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.NAVIGATION);
        expect(uiEventAttributes.get("page")).toEqual("collection_likes");
    }

    @Test
    public void shouldCreateEventFromPlaylistsNavigation() {
        UIEvent uiEvent = UIEvent.fromPlaylistsNav();
        Map<String, String> uiEventAttributes = uiEvent.getAttributes();
        expect(uiEvent.getKind()).toEqual(UIEvent.Kind.NAVIGATION);
        expect(uiEventAttributes.get("page")).toEqual("collection_playlists");
    }

    @Test
    public void shouldCreateEventFromSearchNavigation() {
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
