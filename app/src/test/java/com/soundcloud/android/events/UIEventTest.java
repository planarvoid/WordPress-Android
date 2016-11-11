package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.discovery.charts.ChartSourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.stations.StationsSourceInfo;
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
    private static final Urn QUERY_URN = Urn.forTrack(123L);
    private static final int QUERY_POSITION = 0;
    private TrackSourceInfo trackSourceInfo;
    private PromotedSourceInfo promotedSourceInfo;
    private PromotedSourceInfo promotedSourceInfoWithNoPromoter;
    private EntityMetadata trackMetadata;
    private EntityMetadata playlistMetadata;

    @Before
    public void setUp() throws Exception {
        trackSourceInfo = new TrackSourceInfo("origin screen", true);
        promotedSourceInfo = new PromotedSourceInfo("dfp:ad:123", TRACK_URN, Optional.of(PROMOTER_URN), null);
        promotedSourceInfoWithNoPromoter = new PromotedSourceInfo("dfp:ad:123",
                                                                  TRACK_URN,
                                                                  Optional.<Urn>absent(),
                                                                  null);
        trackMetadata = EntityMetadata.from(buildPlayablePropertySet(TRACK_URN));
        playlistMetadata = EntityMetadata.from(buildPlayablePropertySet(PLAYLIST_URN));
    }

    @Test
    public void shouldCreateEventFromManualPlayerClose() {
        final UIEvent uiEvent = UIEvent.fromPlayerClose(true);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_PLAYER_CLOSE);
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_TRIGGER)).isEqualTo("manual");
    }

    @Test
    public void shouldCreateEventFromManualPlayerOpen() {
        final UIEvent uiEvent = UIEvent.fromPlayerOpen(true);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_PLAYER_OPEN);
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_TRIGGER)).isEqualTo("manual");
    }

    @Test
    public void shouldCreateEventFromAutomaticPlayerClose() {
        final UIEvent uiEvent = UIEvent.fromPlayerClose(false);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_PLAYER_CLOSE);
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_TRIGGER)).isEqualTo("auto");
    }

    @Test
    public void shouldCreateEventFromAutomaticPlayerOpen() {
        final UIEvent uiEvent = UIEvent.fromPlayerOpen(false);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_PLAYER_OPEN);
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_TRIGGER)).isEqualTo("auto");
    }

    @Test
    public void shouldCreateEventFromToggleToFollow() {
        PropertySet userProperties = buildUserPropertySet(Urn.forUser(30L));
        UIEvent uiEvent = UIEvent.fromToggleFollow(true, EntityMetadata.fromUser(userProperties), EventContextMetadata.builder().build());
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_FOLLOW);
        assertThat(uiEvent.get("creator_urn")).isEqualTo("soundcloud:users:30");
        assertThat(uiEvent.get("creator_display_name")).isEqualTo("some username");
    }

    @Test
    public void shouldCreateEventFromToggleToUnfollow() {
        PropertySet userProperties = buildUserPropertySet(Urn.forUser(30L));
        UIEvent uiEvent = UIEvent.fromToggleFollow(false, EntityMetadata.fromUser(userProperties), EventContextMetadata.builder().build());
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNFOLLOW);
    }

    @Test
    public void shouldCreateEventFromLikedTrackToggle() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, TRACK_URN, eventContext, null, trackMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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
        UIEvent uiEvent = UIEvent.fromToggleLike(true,
                                                 TRACK_URN,
                                                 eventContext,
                                                 promotedSourceInfoWithNoPromoter,
                                                 trackMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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
        UIEvent uiEvent = UIEvent.fromToggleLike(true,
                                                 PLAYLIST_URN,
                                                 eventContext,
                                                 promotedSourceInfo,
                                                 playlistMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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
        UIEvent uiEvent = UIEvent.fromToggleLike(true,
                                                 PLAYLIST_URN,
                                                 eventContext,
                                                 promotedSourceInfoWithNoPromoter,
                                                 playlistMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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
        UIEvent uiEvent = UIEvent.fromToggleLike(true,
                                                 PLAYLIST_URN,
                                                 eventContext,
                                                 promotedSourceInfo,
                                                 playlistMetadata);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_LIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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
        UIEvent uiEvent = UIEvent.fromToggleLike(false,
                                                 TRACK_URN,
                                                 eventContext,
                                                 promotedSourceInfoWithNoPromoter,
                                                 trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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
        UIEvent uiEvent = UIEvent.fromToggleLike(false,
                                                 PLAYLIST_URN,
                                                 eventContext,
                                                 promotedSourceInfo,
                                                 playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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
        UIEvent uiEvent = UIEvent.fromToggleLike(false,
                                                 PLAYLIST_URN,
                                                 eventContext,
                                                 promotedSourceInfo,
                                                 playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNLIKE);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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

        EventContextMetadata eventContext = eventContextNoInvokerScreen().trackSourceInfo(info)
                                                                         .isFromOverflow(true)
                                                                         .build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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
        UIEvent uiEvent = UIEvent.fromToggleRepost(true,
                                                   TRACK_URN,
                                                   eventContext,
                                                   promotedSourceInfoWithNoPromoter,
                                                   trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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
        UIEvent uiEvent = UIEvent.fromToggleRepost(true,
                                                   PLAYLIST_URN,
                                                   eventContext,
                                                   promotedSourceInfo,
                                                   playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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
        UIEvent uiEvent = UIEvent.fromToggleRepost(true,
                                                   PLAYLIST_URN,
                                                   eventContext,
                                                   promotedSourceInfo,
                                                   playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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
        UIEvent uiEvent = UIEvent.fromToggleRepost(true,
                                                   PLAYLIST_URN,
                                                   eventContext,
                                                   promotedSourceInfoWithNoPromoter,
                                                   playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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
        UIEvent uiEvent = UIEvent.fromToggleRepost(false,
                                                   TRACK_URN,
                                                   eventContext,
                                                   promotedSourceInfoWithNoPromoter,
                                                   trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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
        UIEvent uiEvent = UIEvent.fromToggleRepost(false,
                                                   PLAYLIST_URN,
                                                   eventContext,
                                                   promotedSourceInfo,
                                                   playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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
        UIEvent uiEvent = UIEvent.fromToggleRepost(false,
                                                   PLAYLIST_URN,
                                                   eventContext,
                                                   promotedSourceInfo,
                                                   playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

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
        UIEvent uiEvent = UIEvent.fromToggleRepost(false,
                                                   PLAYLIST_URN,
                                                   eventContext,
                                                   promotedSourceInfoWithNoPromoter,
                                                   playlistMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:playlists:42");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");

        assertThat(uiEvent.get("ad_urn")).isEqualTo("dfp:ad:123");
        assertThat(uiEvent.get("monetization_type")).isEqualTo("promoted");
        assertThat(uiEvent.get("promoter_urn")).isNull();
    }

    @Test
    public void shouldCreateEventFromAddToPlaylist() {
        UIEvent uiEvent = UIEvent.fromAddToPlaylist(eventContextBuilder().build());
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_ADD_TO_PLAYLIST);
        assertThat(uiEvent.getInvokerScreen()).isEqualTo("invoker_screen");
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
    }

    @Test
    public void shouldCreateEventFromComment() {
        EventContextMetadata eventContextMetadata = EventContextMetadata.builder().contextScreen("screen").build();
        UIEvent uiEvent = UIEvent.fromComment(eventContextMetadata, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_COMMENT);
        assertThat(uiEvent.getContextScreen()).isEqualTo("screen");
        assertThat(uiEvent.get("playable_title")).isEqualTo("some title");
    }

    @Test
    public void shouldCreateEventFromTrackShare() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(TRACK_URN).build();
        UIEvent uiEvent = UIEvent.fromShare(TRACK_URN, eventContext, promotedSourceInfo, trackMetadata);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_SHARE);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");
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
        assertThat(uiEvent.get("creator_display_name")).isEqualTo("some username");
        assertThat(uiEvent.get("creator_urn")).isEqualTo(USER_URN.toString());
        assertThat(uiEvent.get("playable_title")).isEqualTo("some title");
        assertThat(uiEvent.get("playable_urn")).isEqualTo(PLAYLIST_URN.toString());
    }

    @Test
    public void shouldCreateEventFromShuffle() {
        assertThat(UIEvent.fromShuffle(eventContextBuilder().build()).getKind()).isEqualTo(UIEvent.KIND_SHUFFLE);
    }

    @Test
    public void shouldCreateEventFromVideoAdFullScreenClick() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(321L));
        UIEvent uiEvent = UIEvent.fromVideoAdFullscreen(videoAd, trackSourceInfo);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_VIDEO_AD_FULLSCREEN);
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(videoAd.getAdUrn().toString());
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(321).toString());
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("video_ad");
        assertThat(uiEvent.getVideoSizeChangeUrls()).contains("video_fullscreen1", "video_fullscreen2");
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN)).isEqualTo("origin screen");
    }

    @Test
    public void shouldCreateEventFromVideoAdShrinkClick() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(321L));
        UIEvent uiEvent = UIEvent.fromVideoAdShrink(videoAd, trackSourceInfo);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_VIDEO_AD_SHRINK);
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(videoAd.getAdUrn().toString());
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(321).toString());
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("video_ad");
        assertThat(uiEvent.getVideoSizeChangeUrls()).contains("video_exit_full1", "video_exit_full2");
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN)).isEqualTo("origin screen");
    }

    @Test
    public void shouldCreateEventFromVideoAdClickThrough() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(321L));
        UIEvent uiEvent = UIEvent.fromPlayerAdClickThrough(videoAd, trackSourceInfo);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_AD_CLICKTHROUGH);
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(videoAd.getAdUrn().toString());
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(321).toString());
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("video_ad");
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_CLICK_THROUGH_URL)).isEqualTo("http://clickthrough.videoad.com");
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_CLICK_THOUGH_KIND)).isEqualTo("clickthrough::video_ad");
        assertThat(uiEvent.getAdClickthroughUrls()).contains("video_click1", "video_click2");
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN)).isEqualTo("origin screen");
    }

    @Test
    public void shouldCreateEventFromAudioAdClickThrough() {
        AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(321L));
        UIEvent uiEvent = UIEvent.fromPlayerAdClickThrough(audioAd, trackSourceInfo);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_AD_CLICKTHROUGH);
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.getAdUrn().toString());
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(321).toString());
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("audio_ad");
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_CLICK_THROUGH_URL)).isEqualTo("http://clickthrough.visualad.com");
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_CLICK_THOUGH_KIND)).isEqualTo("clickthrough::audio_ad");
        assertThat(uiEvent.getAdClickthroughUrls()).contains("comp_click1", "comp_click2");
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN)).isEqualTo("origin screen");
    }

    @Test
    public void shouldCreateEventFromAppInstallClickThrough() {
        AppInstallAd appInstall = AppInstallAd.create(AdFixtures.getApiAppInstall());
        UIEvent uiEvent = UIEvent.fromAppInstallAdClickThrough(appInstall);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_AD_CLICKTHROUGH);
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(appInstall.getAdUrn().toString());
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("mobile_inlay");
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_CLICK_THROUGH_URL)).isEqualTo("http://clickthrough.com");
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_CLICK_THOUGH_KIND)).isEqualTo("clickthrough::app_install");
        assertThat(uiEvent.getAdClickthroughUrls()).contains("app_click1", "app_click2");
    }

    @Test
    public void shouldCreateEventFromVideoAdSkip() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(321L));
        UIEvent uiEvent = UIEvent.fromSkipAdClick(videoAd, trackSourceInfo);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_SKIP_AD_CLICK);
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(videoAd.getAdUrn().toString());
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(321).toString());
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("video_ad");
        assertThat(uiEvent.getAdSkipUrls()).contains("video_skip1", "video_skip2");
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN)).isEqualTo("origin screen");
    }

    @Test
    public void shouldCreateEventFromAudioAdSkip() {
        AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(321L));
        UIEvent uiEvent = UIEvent.fromSkipAdClick(audioAd, trackSourceInfo);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_SKIP_AD_CLICK);
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.getAdUrn().toString());
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(321).toString());
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("audio_ad");
        assertThat(uiEvent.getAdSkipUrls()).contains("audio_skip1", "audio_skip2");
        assertThat(uiEvent.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN)).isEqualTo("origin screen");
    }

    @Test
    public void shouldCreateEventFromCreatePlaylist() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        UIEvent event = UIEvent.fromCreatePlaylist(EntityMetadata.from(playlist));

        assertThat(event.getKind()).isEqualTo(UIEvent.KIND_CREATE_PLAYLIST);
        assertThat(event.get(EntityMetadata.KEY_PLAYABLE_TITLE)).isEqualTo(playlist.getTitle());
        assertThat(event.get(EntityMetadata.KEY_PLAYABLE_URN)).isEqualTo(playlist.getUrn().toString());
    }

    @Test
    public void shouldCreateEventFromStartStation() {
        UIEvent event = UIEvent.fromStartStation();

        assertThat(event.getKind()).isEqualTo(UIEvent.KIND_START_STATION);
    }

    @Test
    public void shouldCreateEventFromNavigation() {
        final AttributingActivity attributingActivity = AttributingActivity.create(AttributingActivity.POSTED,
                                                                                   Optional.<Urn>absent());

        EventContextMetadata eventContext = eventContextNoInvokerScreen()
                .attributingActivity(attributingActivity)
                .linkType(LinkType.OWNER)
                .build();

        UIEvent uiEvent = UIEvent.fromNavigation(TRACK_URN, eventContext);

        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_NAVIGATION);
        assertThat(uiEvent.getContextScreen()).isEqualTo("context_screen");

        assertThat(uiEvent.get("page_urn")).isEqualTo("soundcloud:unknown:-1");
        assertThat(uiEvent.get("click_object_urn")).isEqualTo("soundcloud:tracks:30");
        assertThat(uiEvent.get("origin_screen")).isEqualTo("page_name");
        assertThat(uiEvent.getAttributingActivity().isPresent()).isTrue();
        assertThat(uiEvent.getAttributingActivity().get()).isEqualTo(attributingActivity);
        assertThat(uiEvent.getLinkType()).isEqualTo(LinkType.OWNER.getName());
    }

    @Test
    public void shouldHaveQueryUrnWhenHasStationsSourceInfo() {
        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.STATIONS_INFO.get(), false);
        final EventContextMetadata eventContextMetadata = EventContextMetadata.builder().trackSourceInfo(trackSourceInfo).build();

        trackSourceInfo.setStationSourceInfo(Urn.forArtistStation(123L), StationsSourceInfo.create(QUERY_URN));

        final UIEvent event = UIEvent.fromToggleLike(true, Urn.NOT_SET, eventContextMetadata, null, EntityMetadata.EMPTY);
        assertThat(event.getQueryUrn()).isEqualTo(Optional.of(QUERY_URN));
    }

    @Test
    public void shouldHaveQueryUrnAndPositionWhenHasChartSourceInfo() {
        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.UNKNOWN.get(), false);
        final EventContextMetadata eventContextMetadata = EventContextMetadata.builder().trackSourceInfo(trackSourceInfo).build();

        trackSourceInfo.setChartSourceInfo(ChartSourceInfo.create(QUERY_POSITION, QUERY_URN,
                                                                  ChartType.TOP,
                                                                  ChartCategory.MUSIC,
                                                                  new Urn("genre")));

        final UIEvent event = UIEvent.fromToggleLike(true, Urn.NOT_SET, eventContextMetadata, null, EntityMetadata.EMPTY);
        assertThat(event.getQueryUrn()).isEqualTo(Optional.of(QUERY_URN));
        assertThat(event.getQueryPosition()).isEqualTo(Optional.of(QUERY_POSITION));
    }

    @Test
    public void shouldHaveAbsentQueryUrnAndPositionIfNoReleventSourceInfo() {
        final EventContextMetadata eventContextMetadata = EventContextMetadata.builder().build();


        final UIEvent event = UIEvent.fromToggleLike(true, Urn.NOT_SET, eventContextMetadata, null, EntityMetadata.EMPTY);
        assertThat(event.getQueryUrn()).isEqualTo(Optional.<TrackSourceInfo>absent());
        assertThat(event.getQueryPosition()).isEqualTo(Optional.<Integer>absent());
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
