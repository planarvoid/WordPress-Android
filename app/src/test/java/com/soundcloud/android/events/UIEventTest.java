package com.soundcloud.android.events;


import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;

public class UIEventTest extends AndroidUnitTest {

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

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_PLAYER_CLOSE);
        assertThat(uiEvent.get("method")).isEqualTo("tap_footer");
    }

    @Test
    public void shouldCreateEventFromPlayerOpen() {
        UIEvent uiEvent = UIEvent.fromPlayerOpen("tap_footer");

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_PLAYER_OPEN);
        assertThat(uiEvent.get("method")).isEqualTo("tap_footer");
    }

    @Test
    public void shouldCreateEventFromToggleToFollow() {
        UIEvent uiEvent = UIEvent.fromToggleFollow(true, "screen", 30);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_FOLLOW);
        assertThat(uiEvent.get("context")).isEqualTo("screen");
        assertThat(uiEvent.get("user_id")).isEqualTo("30");
    }

    @Test
    public void shouldCreateEventFromToggleToUnfollow() {
        UIEvent uiEvent = UIEvent.fromToggleFollow(false, "screen", 30);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNFOLLOW);
        assertThat(uiEvent.get("context")).isEqualTo("screen");
        assertThat(uiEvent.get("user_id")).isEqualTo("30");
    }

    @Test
    public void shouldCreateEventFromLikedToggle() {
        UIEvent uiEvent = UIEvent.fromToggleLike(true, "invoker_screen", "context_screen", TRACK_URN);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.get("location")).isEqualTo("invoker_screen");
        assertThat(uiEvent.get("context")).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");
    }

    @Test
    public void shouldCreateEventFromLikedPlaylist() {
        UIEvent uiEvent = UIEvent.fromToggleLike(true, "invoker_screen", "context_screen", PLAYLIST_URN);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.get("location")).isEqualTo("invoker_screen");
        assertThat(uiEvent.get("context")).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");
    }

    @Test
    public void shouldCreateEventFromUnlikedTrack() {
        UIEvent uiEvent = UIEvent.fromToggleLike(false, "invoker_screen", "context_screen", TRACK_URN);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.get("location")).isEqualTo("invoker_screen");
        assertThat(uiEvent.get("context")).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");
    }

    @Test
    public void shouldCreateEventFromUnlikedPlaylist() {
        UIEvent uiEvent = UIEvent.fromToggleLike(false, "invoker_screen", "context_screen", PLAYLIST_URN);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.get("location")).isEqualTo("invoker_screen");
        assertThat(uiEvent.get("context")).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");
    }

    @Test
    public void shouldCreateEventWithUnknownResourceForUnexpectedUrnType() {
        UIEvent uiEvent = UIEvent.fromToggleLike(true, "invoker_screen", "context_screen", USER_URN);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.get("location")).isEqualTo("invoker_screen");
        assertThat(uiEvent.get("context")).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("unknown");
        assertThat(uiEvent.get("resource_id")).isEqualTo("2");
    }

    @Test
    public void shouldCreateEventFromRepostedTrack() {
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, "screen", TRACK_URN);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.get("context")).isEqualTo("screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");
    }

    @Test
    public void shouldCreateEventFromRepostedPlaylist() {
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, "screen", PLAYLIST_URN);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.get("context")).isEqualTo("screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");
    }

    @Test
    public void shouldCreateEventFromUnrepostedTrack() {
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, "screen", TRACK_URN);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.get("context")).isEqualTo("screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");
    }

    @Test
    public void shouldCreateEventFromUnrepostedPlaylist() {
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, "screen", PLAYLIST_URN);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.get("context")).isEqualTo("screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistIsNew() {
        UIEvent uiEvent = UIEvent.fromAddToPlaylist("invoker_screen", "context_screen", true, 30);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_ADD_TO_PLAYLIST);
        assertThat(uiEvent.get("location")).isEqualTo("invoker_screen");
        assertThat(uiEvent.get("context")).isEqualTo("context_screen");
        assertThat(uiEvent.get("is_new_playlist")).isEqualTo("yes");
        assertThat(uiEvent.get("track_id")).isEqualTo("30");
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistExisted() {
        UIEvent uiEvent = UIEvent.fromAddToPlaylist("invoker_screen", "context_screen", false, 30);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_ADD_TO_PLAYLIST);
        assertThat(uiEvent.get("location")).isEqualTo("invoker_screen");
        assertThat(uiEvent.get("context")).isEqualTo("context_screen");
        assertThat(uiEvent.get("is_new_playlist")).isEqualTo("no");
        assertThat(uiEvent.get("track_id")).isEqualTo("30");
    }

    @Test
    public void shouldCreateEventFromComment() {
        UIEvent uiEvent = UIEvent.fromComment("screen", 30);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_COMMENT);
        assertThat(uiEvent.get("context")).isEqualTo("screen");
        assertThat(uiEvent.get("track_id")).isEqualTo("30");
    }

    @Test
    public void shouldCreateEventFromTrackShare() {
        UIEvent uiEvent = UIEvent.fromShare("screen", TRACK_URN);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_SHARE);
        assertThat(uiEvent.get("context")).isEqualTo("screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");
    }

    @Test
    public void shouldCreateEventFromPlaylistShare() {
        UIEvent uiEvent = UIEvent.fromShare("screen", PLAYLIST_URN);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_SHARE);
        assertThat(uiEvent.get("context")).isEqualTo("screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");
    }

    @Test
    public void shouldCreateEventFromShuffleMyLikes() {
        assertThat(UIEvent.fromShuffleMyLikes().getKind()).isEqualTo(UIEvent.KIND_SHUFFLE_LIKES);
    }

    @Test
    public void shouldCreateEventFromProfileNavigation() {
        UIEvent uiEvent = UIEvent.fromProfileNav();
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_NAVIGATION);
        assertThat(uiEvent.get("page")).isEqualTo("you");
    }

    @Test
    public void shouldCreateEventFromStreamNavigation() {
        UIEvent uiEvent = UIEvent.fromStreamNav();
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_NAVIGATION);
        assertThat(uiEvent.get("page")).isEqualTo("stream");
    }

    @Test
    public void shouldCreateEventFromExploreNavigation() {
        UIEvent uiEvent = UIEvent.fromExploreNav();
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_NAVIGATION);
        assertThat(uiEvent.get("page")).isEqualTo("explore");
    }

    @Test
    public void shouldCreateEventFromLikesNavigation() {
        UIEvent uiEvent = UIEvent.fromLikesNav();
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_NAVIGATION);
        assertThat(uiEvent.get("page")).isEqualTo("collection_likes");
    }

    @Test
    public void shouldCreateEventFromPlaylistsNavigation() {
        UIEvent uiEvent = UIEvent.fromPlaylistsNav();
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_NAVIGATION);
        assertThat(uiEvent.get("page")).isEqualTo("collection_playlists");
    }

    @Test
    public void shouldCreateEventFromSearchNavigation() {
        UIEvent uiEvent = UIEvent.fromSearchAction();
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_NAVIGATION);
        assertThat(uiEvent.get("page")).isEqualTo("search");
    }

    @Test
    public void shouldCreateEventFromAudioAdClick() {
        PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123));
        UIEvent uiEvent = UIEvent.fromAudioAdCompanionDisplayClick(audioAd, Urn.forTrack(456), Urn.forUser(456L), trackSourceInfo, 1000L);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_AUDIO_AD_CLICK);
        assertThat(uiEvent.getTimestamp()).isEqualTo(1000L);
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.get(AdProperty.COMPANION_URN));
        assertThat(uiEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123).toString());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_TRACK_URN)).isEqualTo(Urn.forTrack(456).toString());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_CLICK_THROUGH_URL)).isEqualTo(audioAd.get(AdProperty.CLICK_THROUGH_LINK).toString());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_ARTWORK_URL)).isEqualTo(audioAd.get(AdProperty.ARTWORK).toString());
        assertThat(uiEvent.getAudioAdClickthroughUrls()).contains("click1", "click2");
    }

    @Test
    public void shouldCreateEventFromSkipAudioAdClick() {
        PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123));
        UIEvent uiEvent = UIEvent.fromSkipAudioAdClick(audioAd, Urn.forTrack(456), Urn.forUser(456L), trackSourceInfo, 1000L);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_SKIP_AUDIO_AD_CLICK);
        assertThat(uiEvent.getTimestamp()).isEqualTo(1000L);
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.get(AdProperty.AUDIO_AD_URN));
        assertThat(uiEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123).toString());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_TRACK_URN)).isEqualTo(Urn.forTrack(456).toString());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_ARTWORK_URL)).isEqualTo(audioAd.get(AdProperty.ARTWORK).toString());
        assertThat(uiEvent.getAudioAdSkipUrls()).contains("skip1", "skip2");
    }

}
