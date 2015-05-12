package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class UIEventTest {

    private static final Urn TRACK_URN = Urn.forTrack(30L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(42L);
    private static final Urn USER_URN = Urn.forUser(2L);
    private TrackSourceInfo trackSourceInfo;

    @Before
    public void setUp() throws Exception {
        trackSourceInfo = new TrackSourceInfo("origin screen", true);
    }

    @Test
    public void shouldCreateEventFromPlayerClose() {
        UIEvent uiEvent = UIEvent.fromPlayerClose("tap_footer");

        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_PLAYER_CLOSE);
        expect(uiEvent.get("method")).toEqual("tap_footer");
    }

    @Test
    public void shouldCreateEventFromPlayerOpen() {
        UIEvent uiEvent = UIEvent.fromPlayerOpen("tap_footer");

        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_PLAYER_OPEN);
        expect(uiEvent.get("method")).toEqual("tap_footer");
    }

    @Test
    public void shouldCreateEventFromToggleToFollow() {
        UIEvent uiEvent = UIEvent.fromToggleFollow(true, "screen", 30);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_FOLLOW);
        expect(uiEvent.get("context")).toEqual("screen");
        expect(uiEvent.get("user_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromToggleToUnfollow() {
        UIEvent uiEvent = UIEvent.fromToggleFollow(false, "screen", 30);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_UNFOLLOW);
        expect(uiEvent.get("context")).toEqual("screen");
        expect(uiEvent.get("user_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromLikedToggle() {
        UIEvent uiEvent = UIEvent.fromToggleLike(true, "invoker_screen", "context_screen", TRACK_URN);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_LIKE);
        expect(uiEvent.get("location")).toEqual("invoker_screen");
        expect(uiEvent.get("context")).toEqual("context_screen");
        expect(uiEvent.get("resource")).toEqual("track");
        expect(uiEvent.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromLikedPlaylist() {
        UIEvent uiEvent = UIEvent.fromToggleLike(true, "invoker_screen", "context_screen", PLAYLIST_URN);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_LIKE);
        expect(uiEvent.get("location")).toEqual("invoker_screen");
        expect(uiEvent.get("context")).toEqual("context_screen");
        expect(uiEvent.get("resource")).toEqual("playlist");
        expect(uiEvent.get("resource_id")).toEqual("42");
    }

    @Test
    public void shouldCreateEventFromUnlikedTrack() {
        UIEvent uiEvent = UIEvent.fromToggleLike(false, "invoker_screen", "context_screen", TRACK_URN);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_UNLIKE);
        expect(uiEvent.get("location")).toEqual("invoker_screen");
        expect(uiEvent.get("context")).toEqual("context_screen");
        expect(uiEvent.get("resource")).toEqual("track");
        expect(uiEvent.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromUnlikedPlaylist() {
        UIEvent uiEvent = UIEvent.fromToggleLike(false, "invoker_screen", "context_screen", PLAYLIST_URN);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_UNLIKE);
        expect(uiEvent.get("location")).toEqual("invoker_screen");
        expect(uiEvent.get("context")).toEqual("context_screen");
        expect(uiEvent.get("resource")).toEqual("playlist");
        expect(uiEvent.get("resource_id")).toEqual("42");
    }

    @Test
    public void shouldCreateEventWithUnknownResourceForUnexpectedUrnType() {
        UIEvent uiEvent = UIEvent.fromToggleLike(true, "invoker_screen", "context_screen", USER_URN);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_LIKE);
        expect(uiEvent.get("location")).toEqual("invoker_screen");
        expect(uiEvent.get("context")).toEqual("context_screen");
        expect(uiEvent.get("resource")).toEqual("unknown");
        expect(uiEvent.get("resource_id")).toEqual("2");
    }

    @Test
    public void shouldCreateEventFromRepostedTrack() {
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, "screen", TRACK_URN);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_REPOST);
        expect(uiEvent.get("context")).toEqual("screen");
        expect(uiEvent.get("resource")).toEqual("track");
        expect(uiEvent.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromRepostedPlaylist() {
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, "screen", PLAYLIST_URN);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_REPOST);
        expect(uiEvent.get("context")).toEqual("screen");
        expect(uiEvent.get("resource")).toEqual("playlist");
        expect(uiEvent.get("resource_id")).toEqual("42");
    }

    @Test
    public void shouldCreateEventFromUnrepostedTrack() {
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, "screen", TRACK_URN);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_UNREPOST);
        expect(uiEvent.get("context")).toEqual("screen");
        expect(uiEvent.get("resource")).toEqual("track");
        expect(uiEvent.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromUnrepostedPlaylist() {
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, "screen", PLAYLIST_URN);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_UNREPOST);
        expect(uiEvent.get("context")).toEqual("screen");
        expect(uiEvent.get("resource")).toEqual("playlist");
        expect(uiEvent.get("resource_id")).toEqual("42");
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistIsNew() {
        UIEvent uiEvent = UIEvent.fromAddToPlaylist("invoker_screen", "context_screen", true, 30);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_ADD_TO_PLAYLIST);
        expect(uiEvent.get("location")).toEqual("invoker_screen");
        expect(uiEvent.get("context")).toEqual("context_screen");
        expect(uiEvent.get("is_new_playlist")).toEqual("yes");
        expect(uiEvent.get("track_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistExisted() {
        UIEvent uiEvent = UIEvent.fromAddToPlaylist("invoker_screen", "context_screen", false, 30);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_ADD_TO_PLAYLIST);
        expect(uiEvent.get("location")).toEqual("invoker_screen");
        expect(uiEvent.get("context")).toEqual("context_screen");
        expect(uiEvent.get("is_new_playlist")).toEqual("no");
        expect(uiEvent.get("track_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromComment() {
        UIEvent uiEvent = UIEvent.fromComment("screen", 30);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_COMMENT);
        expect(uiEvent.get("context")).toEqual("screen");
        expect(uiEvent.get("track_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromTrackShare() {
        UIEvent uiEvent = UIEvent.fromShare("screen", TRACK_URN);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_SHARE);
        expect(uiEvent.get("context")).toEqual("screen");
        expect(uiEvent.get("resource")).toEqual("track");
        expect(uiEvent.get("resource_id")).toEqual("30");
    }

    @Test
    public void shouldCreateEventFromPlaylistShare() {
        UIEvent uiEvent = UIEvent.fromShare("screen", PLAYLIST_URN);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_SHARE);
        expect(uiEvent.get("context")).toEqual("screen");
        expect(uiEvent.get("resource")).toEqual("playlist");
        expect(uiEvent.get("resource_id")).toEqual("42");
    }

    @Test
    public void shouldCreateEventFromShuffleMyLikes() {
        expect(UIEvent.fromShuffleMyLikes().getKind()).toEqual(UIEvent.KIND_SHUFFLE_LIKES);
    }

    @Test
    public void shouldCreateEventFromProfileNavigation() {
        UIEvent uiEvent = UIEvent.fromProfileNav();
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_NAVIGATION);
        expect(uiEvent.get("page")).toEqual("you");
    }

    @Test
    public void shouldCreateEventFromStreamNavigation() {
        UIEvent uiEvent = UIEvent.fromStreamNav();
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_NAVIGATION);
        expect(uiEvent.get("page")).toEqual("stream");
    }

    @Test
    public void shouldCreateEventFromExploreNavigation() {
        UIEvent uiEvent = UIEvent.fromExploreNav();
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_NAVIGATION);
        expect(uiEvent.get("page")).toEqual("explore");
    }

    @Test
    public void shouldCreateEventFromLikesNavigation() {
        UIEvent uiEvent = UIEvent.fromLikesNav();
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_NAVIGATION);
        expect(uiEvent.get("page")).toEqual("collection_likes");
    }

    @Test
    public void shouldCreateEventFromPlaylistsNavigation() {
        UIEvent uiEvent = UIEvent.fromPlaylistsNav();
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_NAVIGATION);
        expect(uiEvent.get("page")).toEqual("collection_playlists");
    }

    @Test
    public void shouldCreateEventFromSearchNavigation() {
        UIEvent uiEvent = UIEvent.fromSearchAction();
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_NAVIGATION);
        expect(uiEvent.get("page")).toEqual("search");
    }

    @Test
    public void shouldCreateEventFromAudioAdClick() {
        PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123));
        UIEvent uiEvent = UIEvent.fromAudioAdCompanionDisplayClick(audioAd, Urn.forTrack(456), Urn.forUser(456L), trackSourceInfo, 1000L);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_AUDIO_AD_CLICK);
        expect(uiEvent.getTimestamp()).toEqual(1000L);
        expect(uiEvent.get(AdTrackingKeys.KEY_AD_URN)).toEqual(audioAd.get(AdProperty.COMPANION_URN));
        expect(uiEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).toEqual(Urn.forTrack(123).toString());
        expect(uiEvent.get(AdTrackingKeys.KEY_AD_TRACK_URN)).toEqual(Urn.forTrack(456).toString());
        expect(uiEvent.get(AdTrackingKeys.KEY_CLICK_THROUGH_URL)).toEqual(audioAd.get(AdProperty.CLICK_THROUGH_LINK).toString());
        expect(uiEvent.get(AdTrackingKeys.KEY_AD_ARTWORK_URL)).toEqual(audioAd.get(AdProperty.ARTWORK).toString());
        expect(uiEvent.getAudioAdClickthroughUrls()).toContain("click1", "click2");
    }

    @Test
    public void shouldCreateEventFromSkipAudioAdClick() {
        PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123));
        UIEvent uiEvent = UIEvent.fromSkipAudioAdClick(audioAd, Urn.forTrack(456), Urn.forUser(456L), trackSourceInfo, 1000L);
        expect(uiEvent.getKind()).toEqual(UIEvent.KIND_SKIP_AUDIO_AD_CLICK);
        expect(uiEvent.getTimestamp()).toEqual(1000L);
        expect(uiEvent.get(AdTrackingKeys.KEY_AD_URN)).toEqual(audioAd.get(AdProperty.AUDIO_AD_URN));
        expect(uiEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).toEqual(Urn.forTrack(123).toString());
        expect(uiEvent.get(AdTrackingKeys.KEY_AD_TRACK_URN)).toEqual(Urn.forTrack(456).toString());
        expect(uiEvent.get(AdTrackingKeys.KEY_AD_ARTWORK_URL)).toEqual(audioAd.get(AdProperty.ARTWORK).toString());
        expect(uiEvent.getAudioAdSkipUrls()).toContain("skip1", "skip2");
    }

}
