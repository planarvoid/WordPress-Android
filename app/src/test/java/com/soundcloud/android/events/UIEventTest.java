package com.soundcloud.android.events;


import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;

public class UIEventTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(30L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(42L);
    private static final Urn USER_URN = Urn.forUser(2L);
    private static final Urn PROMOTER_URN = Urn.forUser(21L);
    private TrackSourceInfo trackSourceInfo;
    private PromotedSourceInfo promotedSourceInfo;
    private PromotedSourceInfo promotedSourceInfoWithNoPromoter;
    private EntityMetadata trackMetadata;
    private EntityMetadata playlistMetadata;

    @Before
    public void setUp() throws Exception {
        trackSourceInfo = new TrackSourceInfo("origin screen", true);
        promotedSourceInfo = new PromotedSourceInfo("dfp:ad:123", TRACK_URN, Optional.of(PROMOTER_URN), null);
        promotedSourceInfoWithNoPromoter = new PromotedSourceInfo("dfp:ad:123", TRACK_URN, Optional.<Urn>absent(), null);
        trackMetadata = EntityMetadata.from(buildPlayablePropertySet(TRACK_URN));
        playlistMetadata = EntityMetadata.from(buildPlayablePropertySet(PLAYLIST_URN));
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
        PropertySet userProperties = buildUserPropertySet(Urn.forUser(30l));
        UIEvent uiEvent = UIEvent.fromToggleFollow(true, EntityMetadata.fromUser(userProperties));
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_FOLLOW);
        assertThat(uiEvent.get("creator_urn")).isEqualTo("soundcloud:users:30");
        assertThat(uiEvent.get("creator_display_name")).isEqualTo("some username");
    }

    @Test
    public void shouldCreateEventFromToggleToUnfollow() {
        PropertySet userProperties = buildUserPropertySet(Urn.forUser(30l));
        UIEvent uiEvent = UIEvent.fromToggleFollow(false, EntityMetadata.fromUser(userProperties));
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNFOLLOW);
    }

    @Test
    public void shouldCreateEventFromLikedTrackToggle() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, TRACK_URN, eventContext, null, trackMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();

        assertThat(uiEvent.get("playable_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("playable_title")).isEqualTo("some title");
        assertThat(uiEvent.get("creator_display_name")).isEqualTo("some username");
        assertThat(uiEvent.get("creator_urn")).isEqualTo("soundcloud:users:2");
    }

    @Test
    public void shouldCreateEventFromLikedTrackToggleWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, TRACK_URN, eventContext, null, trackMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromLikedTrackToggleWithTrackPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(TRACK_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, TRACK_URN, eventContext, null, trackMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromLikedPromotedTrackToggle() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, TRACK_URN, eventContext, promotedSourceInfo, trackMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isEqualTo("soundcloud:users:21");
    }

    @Test
    public void shouldCreateEventFromLikedPromotedTrackToggleWithTrackPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(TRACK_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, TRACK_URN, eventContext, promotedSourceInfo, trackMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isEqualTo("soundcloud:users:21");
    }

    @Test
    public void shouldCreateEventFromLikedPromotedTrackToggleWithNoPromoter() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, TRACK_URN, eventContext, promotedSourceInfoWithNoPromoter, trackMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromLikedPlaylist() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, PLAYLIST_URN, eventContext, null, playlistMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();

        assertThat(uiEvent.get("playable_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("playable_title")).isEqualTo("some title");
        assertThat(uiEvent.get("creator_display_name")).isEqualTo("some username");
        assertThat(uiEvent.get("creator_urn")).isEqualTo("soundcloud:users:2");
    }

    @Test
    public void shouldCreateEventFromLikedPlaylistWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, PLAYLIST_URN, eventContext, null, playlistMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromLikedPromotedPlaylist() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, PLAYLIST_URN, eventContext, promotedSourceInfo, playlistMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isEqualTo("soundcloud:users:21");
    }

    @Test
    public void shouldCreateEventFromLikedPromotedPlaylistWithNoPromoter() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, PLAYLIST_URN, eventContext, promotedSourceInfoWithNoPromoter, playlistMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromLikedPromotedPlaylistWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, PLAYLIST_URN, eventContext, promotedSourceInfo, playlistMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isEqualTo("soundcloud:users:21");
    }

    @Test
    public void shouldCreateEventFromUnlikedTrack() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromUnlikedTrackWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromUnlikedTrackWithTrackPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(TRACK_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromUnlikedPromotedTrack() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false, TRACK_URN, eventContext, promotedSourceInfo, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isEqualTo("soundcloud:users:21");
    }

    @Test
    public void shouldCreateEventFromUnlikedPromotedTrackWithNoPromoter() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false, TRACK_URN, eventContext, promotedSourceInfoWithNoPromoter, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromUnlikedPlaylist() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false, PLAYLIST_URN, eventContext, null, playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromUnlikedPlaylistWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false, PLAYLIST_URN, eventContext, null, playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromUnlikedPromotedPlaylist() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false, PLAYLIST_URN, eventContext, promotedSourceInfo, playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isEqualTo("soundcloud:users:21");
    }

    @Test
    public void shouldCreateEventFromUnlikedPromotedPlaylistWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false, PLAYLIST_URN, eventContext, promotedSourceInfo, playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isEqualTo("soundcloud:users:21");
    }

    @Test
    public void shouldCreateEventWithUnknownResourceForUnexpectedUrnType() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, USER_URN, eventContext, null, EntityMetadata.EMPTY);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("unknown");
        assertThat(uiEvent.get("resource_id")).isEqualTo("2");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:users:2");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromRepostedTrack() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();

        assertThat(uiEvent.get("creator_display_name")).isEqualTo("some username");
        assertThat(uiEvent.get("creator_urn")).isEqualTo(USER_URN.toString());
        assertThat(uiEvent.get("playable_title")).isEqualTo("some title");
        assertThat(uiEvent.get("playable_urn")).isEqualTo(TRACK_URN.toString());
    }

    @Test
    public void shouldCreateEventFromRepostedTrackFromOverflowMenu() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().isFromOverflow(true).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();

        assertThat(uiEvent.get("creator_display_name")).isEqualTo("some username");
        assertThat(uiEvent.get("creator_urn")).isEqualTo(USER_URN.toString());
        assertThat(uiEvent.get("playable_title")).isEqualTo("some title");
        assertThat(uiEvent.get("playable_urn")).isEqualTo(TRACK_URN.toString());

        assertThat(uiEvent.isFromOverflow()).isTrue();
    }

    @Test
    public void shouldCreateEventFromRepostedTrackFromOverflowWithClickSource() {
        TrackSourceInfo info = new TrackSourceInfo(Screen.STREAM.get(), true);
        info.setSource("stream", "");

        EventContextMetadata eventContext = eventContextNoInvokerScreen().trackSourceInfo(info).isFromOverflow(true).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();

        assertThat(uiEvent.get("creator_display_name")).isEqualTo("some username");
        assertThat(uiEvent.get("creator_urn")).isEqualTo(USER_URN.toString());
        assertThat(uiEvent.get("playable_title")).isEqualTo("some title");
        assertThat(uiEvent.get("playable_urn")).isEqualTo(TRACK_URN.toString());

        assertThat(uiEvent.isFromOverflow()).isTrue();
        assertThat(uiEvent.getClickSource()).isEqualTo("stream");
    }

    @Test
    public void shouldCreateEventFromRepostedTrackWithTrackPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(TRACK_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromRepostedTrackWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromRepostedPromotedTrack() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, promotedSourceInfo, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isEqualTo("soundcloud:users:21");
    }

    @Test
    public void shouldCreateEventFromRepostedPromotedTrackWithNoPromoter() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, promotedSourceInfoWithNoPromoter, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromRepostedPlaylist() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, PLAYLIST_URN, eventContext, null, playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();

        assertThat(uiEvent.get("creator_display_name")).isEqualTo("some username");
        assertThat(uiEvent.get("creator_urn")).isEqualTo(USER_URN.toString());
        assertThat(uiEvent.get("playable_title")).isEqualTo("some title");
        assertThat(uiEvent.get("playable_urn")).isEqualTo(PLAYLIST_URN.toString());
    }

    @Test
    public void shouldCreateEventFromRepostedPlaylistWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, PLAYLIST_URN, eventContext, null, playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromRepostedPromotedPlaylist() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, PLAYLIST_URN, eventContext, promotedSourceInfo, playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isEqualTo("soundcloud:users:21");
    }

    @Test
    public void shouldCreateEventFromRepostedPromotedPlaylistWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, PLAYLIST_URN, eventContext, promotedSourceInfo, playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isEqualTo("soundcloud:users:21");
    }

    @Test
    public void shouldCreateEventFromRepostedPromotedPlaylistWithNoPromoter() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, PLAYLIST_URN, eventContext, promotedSourceInfoWithNoPromoter, playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromUnrepostedTrack() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromUnrepostedTrackWithTrackPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(TRACK_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromUnrepostedTrackWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromUnrepostedPromotedTrack() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, TRACK_URN, eventContext, promotedSourceInfo, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isEqualTo("soundcloud:users:21");
    }

    @Test
    public void shouldCreateEventFromUnrepostedPromotedTrackWithTrackPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(TRACK_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, TRACK_URN, eventContext, promotedSourceInfo, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isEqualTo("soundcloud:users:21");
    }

    @Test
    public void shouldCreateEventFromUnrepostedPromotedTrackWithNoPromoter() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, TRACK_URN, eventContext, promotedSourceInfoWithNoPromoter, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromUnrepostedPlaylist() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, PLAYLIST_URN, eventContext, null, playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isNull();
        assertThat(uiEvent.get("monetization_type")).isNull();
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromUnrepostedPromotedPlaylist() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, PLAYLIST_URN, eventContext, promotedSourceInfo, playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isEqualTo("soundcloud:users:21");
    }

    @Test
    public void shouldCreateEventFromUnrepostedPromotedPlaylistWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, PLAYLIST_URN, eventContext, promotedSourceInfo, playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isEqualTo("soundcloud:users:21");
    }

    @Test
    public void shouldCreateEventFromUnrepostedPromotedPlaylistWithNoPromoter() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, PLAYLIST_URN, eventContext, promotedSourceInfoWithNoPromoter, playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistIsNew() {
        UIEvent uiEvent = UIEvent.fromAddToPlaylist(eventContextBuilder().build(), true, 30);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_ADD_TO_PLAYLIST);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("is_new_playlist")).isEqualTo("yes");
        assertThat(uiEvent.get("track_id")).isEqualTo("30");
    }

    @Test
    public void shouldCreateEventFromAddToPlaylistWhenPlaylistExisted() {

        UIEvent uiEvent = UIEvent.fromAddToPlaylist(eventContextBuilder().build(), false, 30);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_ADD_TO_PLAYLIST);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("is_new_playlist")).isEqualTo("no");
        assertThat(uiEvent.get("track_id")).isEqualTo("30");
    }

    @Test
    public void shouldCreateEventFromComment() {
        EventContextMetadata eventContextMetadata = EventContextMetadata.builder().contextScreen("screen").build();
        UIEvent uiEvent = UIEvent.fromComment(eventContextMetadata, 30, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_COMMENT);
        assertThat(uiEvent.getContextScreen()).isEqualTo("screen");
        assertThat(uiEvent.get("track_id")).isEqualTo("30");
        assertThat(uiEvent.get("playable_title")).isEqualTo("some title");
    }

    @Test
    public void shouldCreateEventFromTrackShare() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(TRACK_URN).build();
        UIEvent uiEvent = UIEvent.fromShare(TRACK_URN, eventContext, promotedSourceInfo, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_SHARE);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("track");
        assertThat(uiEvent.get("resource_id")).isEqualTo("30");
        assertThat(uiEvent.get("creator_display_name")).isEqualTo("some username");
        assertThat(uiEvent.get("creator_urn")).isEqualTo(USER_URN.toString());
        assertThat(uiEvent.get("playable_title")).isEqualTo("some title");
        assertThat(uiEvent.get("playable_urn")).isEqualTo(TRACK_URN.toString());
    }

    @Test
    public void shouldCreateEventFromPlaylistShare() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromShare(PLAYLIST_URN, eventContext, promotedSourceInfo, playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_SHARE);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
        assertThat(uiEvent.get("resource")).isEqualTo("playlist");
        assertThat(uiEvent.get("resource_id")).isEqualTo("42");
        assertThat(uiEvent.get("creator_display_name")).isEqualTo("some username");
        assertThat(uiEvent.get("creator_urn")).isEqualTo(USER_URN.toString());
        assertThat(uiEvent.get("playable_title")).isEqualTo("some title");
        assertThat(uiEvent.get("playable_urn")).isEqualTo(PLAYLIST_URN.toString());
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
        AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        UIEvent uiEvent = UIEvent.fromAudioAdCompanionDisplayClick(audioAd, Urn.forTrack(456), Urn.forUser(456L), trackSourceInfo, 1000L);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_AUDIO_AD_CLICK);
        assertThat(uiEvent.getTimestamp()).isEqualTo(1000L);
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.getVisualAd().getAdUrn());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123).toString());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_TRACK_URN)).isEqualTo(Urn.forTrack(456).toString());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_CLICK_THROUGH_URL)).isEqualTo(audioAd.getVisualAd().getClickThroughUrl().toString());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_ARTWORK_URL)).isEqualTo(audioAd.getVisualAd().getImageUrl().toString());
        assertThat(uiEvent.getAudioAdClickthroughUrls()).contains("comp_click1", "comp_click2");
    }

    @Test
    public void shouldCreateEventFromSkipAudioAdClick() {
        AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        UIEvent uiEvent = UIEvent.fromSkipAudioAdClick(audioAd, Urn.forTrack(456), Urn.forUser(456L), trackSourceInfo, 1000L);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_SKIP_AUDIO_AD_CLICK);
        assertThat(uiEvent.getTimestamp()).isEqualTo(1000L);
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.getAdUrn());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123).toString());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_TRACK_URN)).isEqualTo(Urn.forTrack(456).toString());
        assertThat(uiEvent.get(AdTrackingKeys.KEY_AD_ARTWORK_URL)).isEqualTo(audioAd.getVisualAd().getImageUrl().toString());
        assertThat(uiEvent.getAudioAdSkipUrls()).contains("audio_skip1", "audio_skip2");
    }

    @Test
    public void shouldCreateEventFromCreatePlaylist() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        UIEvent event = UIEvent.fromCreatePlaylist(EntityMetadata.from(playlist));

        assertThat(event.getKind()).isEqualTo(UIEvent.KIND_CREATE_PLAYLIST);
        assertThat(event.get(EntityMetadata.KEY_PLAYABLE_TITLE)).isEqualTo(playlist.getTitle());
        assertThat(event.get(EntityMetadata.KEY_PLAYABLE_URN)).isEqualTo(playlist.getUrn().toString());
    }

    private EventContextMetadata.Builder eventContextBuilder() {
        return eventContextNoInvokerScreen().invokerScreen("invoker_screen");
    }

    private EventContextMetadata.Builder eventContextNoInvokerScreen() {
        return EventContextMetadata.builder()
                .contextScreen("context_screen")
                .pageName("page_name")
                .pageUrn(Urn.NOT_SET);
    }

    private PropertySet buildPlayablePropertySet(Urn urn) {
        return PropertySet.from(
                PlayableProperty.URN.bind(urn),
                PlayableProperty.CREATOR_URN.bind(USER_URN),
                PlayableProperty.CREATOR_NAME.bind("some username"),
                PlayableProperty.TITLE.bind("some title")
        );
    }

    private PropertySet buildUserPropertySet(Urn urn) {
        return PropertySet.from(
                UserProperty.URN.bind(urn),
                UserProperty.USERNAME.bind("some username"),
                UserProperty.ID.bind(urn.getNumericId())
        );
    }

}
