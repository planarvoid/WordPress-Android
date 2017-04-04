package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.ads.AdData.MonetizationType;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.discovery.recommendations.QuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager.RepeatMode;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.stations.StationsSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import org.junit.Before;
import org.junit.Test;

public class UIEventTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(30L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(42L);
    private static final Urn USER_URN = Urn.forUser(2L);
    private static final Urn PROMOTER_URN = Urn.forUser(21L);
    private static final Urn QUERY_URN = Urn.forTrack(123L);
    private static final int QUERY_POSITION = 0;
    public static final String CONTEXT_SCREEN = "context_screen";
    public static final Urn CREATOR_URN = Urn.forUser(30L);
    public static final Urn PAGE_URN = new Urn("soundcloud:unknown:-1");
    public static final Urn CLICK_OBJECT_URN = new Urn("soundcloud:tracks:30");
    public static final String ORIGIN_SCREEN_NAME = "page_name";
    public static final String PLAYABLE_TITLE = "some title";
    public static final String CREATOR_NAME = "some username";
    public static final String AD_URN = "dfp:ad:123";
    public static final Urn SOUNDCLOUD_PLAYLISTS_42 = new Urn("soundcloud:playlists:42");
    public static final Urn SOUNDCLOUD_USERS_2 = new Urn("soundcloud:users:2");
    private final String INVOKER_SCREEN = "invoker_screen";
    private TrackSourceInfo trackSourceInfo;
    private PromotedSourceInfo promotedSourceInfo;
    private PromotedSourceInfo promotedSourceInfoWithNoPromoter;
    private EntityMetadata trackMetadata;
    private EntityMetadata playlistMetadata;

    @Before
    public void setUp() throws Exception {
        trackSourceInfo = new TrackSourceInfo("origin screen", true);
        promotedSourceInfo = new PromotedSourceInfo(AD_URN, TRACK_URN, Optional.of(PROMOTER_URN), null);
        promotedSourceInfoWithNoPromoter = new PromotedSourceInfo(AD_URN,
                                                                  TRACK_URN,
                                                                  Optional.absent(),
                                                                  null);
        trackMetadata = buildMetadata(TRACK_URN);
        playlistMetadata = buildMetadata(PLAYLIST_URN);
    }

    @Test
    public void shouldCreateEventFromManualPlayerClose() {
        final UIEvent uiEvent = UIEvent.fromPlayerClose(true);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.PLAYER_CLOSE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.PLAYER_CLOSE);
        assertThat(uiEvent.trigger().get()).isEqualTo(UIEvent.Trigger.MANUAL);
    }

    @Test
    public void shouldCreateEventFromManualPlayerOpen() {
        final UIEvent uiEvent = UIEvent.fromPlayerOpen(true);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.PLAYER_OPEN);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.PLAYER_OPEN);
        assertThat(uiEvent.trigger().get()).isEqualTo(UIEvent.Trigger.MANUAL);
    }

    @Test
    public void shouldCreateEventFromAutomaticPlayerClose() {
        final UIEvent uiEvent = UIEvent.fromPlayerClose(false);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.PLAYER_CLOSE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.PLAYER_CLOSE);
        assertThat(uiEvent.trigger().get()).isEqualTo(UIEvent.Trigger.AUTO);
    }

    @Test
    public void shouldCreateEventFromAutomaticPlayerOpen() {
        final UIEvent uiEvent = UIEvent.fromPlayerOpen(false);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.PLAYER_OPEN);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.PLAYER_OPEN);
        assertThat(uiEvent.trigger().get()).isEqualTo(UIEvent.Trigger.AUTO);
    }

    @Test
    public void shouldCreateEventFromToggleToFollow() {
        UIEvent uiEvent = UIEvent.fromToggleFollow(true, buildUserMetadata(CREATOR_URN), EventContextMetadata.builder().build());
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.FOLLOW);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.FOLLOW_ADD);
        assertThat(uiEvent.creatorUrn().get()).isEqualTo(CREATOR_URN);
        assertThat(uiEvent.creatorName().get()).isEqualTo(CREATOR_NAME);
    }

    @Test
    public void shouldCreateEventFromToggleToUnfollow() {
        UIEvent uiEvent = UIEvent.fromToggleFollow(false, buildUserMetadata(CREATOR_URN), EventContextMetadata.builder().build());
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNFOLLOW);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.FOLLOW_REMOVE);
    }

    @Test
    public void shouldCreateEventFromLikedTrackToggle() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, TRACK_URN, eventContext, null, trackMetadata);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.LIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.LIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();

        assertThat(uiEvent.playableUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.playableTitle().get()).isEqualTo(PLAYABLE_TITLE);
        assertThat(uiEvent.creatorName().get()).isEqualTo(CREATOR_NAME);
        assertThat(uiEvent.creatorUrn().get()).isEqualTo(SOUNDCLOUD_USERS_2);
    }

    @Test
    public void shouldCreateEventFromLikedTrackToggleWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, TRACK_URN, eventContext, null, trackMetadata);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.LIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.LIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromLikedTrackToggleWithTrackPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(TRACK_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, TRACK_URN, eventContext, null, trackMetadata);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.LIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.LIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromLikedPromotedTrackToggle() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, TRACK_URN, eventContext, promotedSourceInfo, trackMetadata);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.LIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.LIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().get()).isEqualTo(PROMOTER_URN);
    }

    @Test
    public void shouldCreateEventFromLikedPromotedTrackToggleWithTrackPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(TRACK_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, TRACK_URN, eventContext, promotedSourceInfo, trackMetadata);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.LIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.LIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().get()).isEqualTo(PROMOTER_URN);
    }

    @Test
    public void shouldCreateEventFromLikedPromotedTrackToggleWithNoPromoter() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true,
                                                 TRACK_URN,
                                                 eventContext,
                                                 promotedSourceInfoWithNoPromoter,
                                                 trackMetadata);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.LIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.LIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromLikedPlaylist() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, PLAYLIST_URN, eventContext, null, playlistMetadata);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.LIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.LIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();

        assertThat(uiEvent.playableUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.playableTitle().get()).isEqualTo(PLAYABLE_TITLE);
        assertThat(uiEvent.creatorName().get()).isEqualTo(CREATOR_NAME);
        assertThat(uiEvent.creatorUrn().get()).isEqualTo(SOUNDCLOUD_USERS_2);
    }

    @Test
    public void shouldCreateEventFromLikedPlaylistWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, PLAYLIST_URN, eventContext, null, playlistMetadata);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.LIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.LIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromLikedPromotedPlaylist() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true,
                                                 PLAYLIST_URN,
                                                 eventContext,
                                                 promotedSourceInfo,
                                                 playlistMetadata);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.LIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.LIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().get()).isEqualTo(PROMOTER_URN);
    }

    @Test
    public void shouldCreateEventFromLikedPromotedPlaylistWithNoPromoter() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true,
                                                 PLAYLIST_URN,
                                                 eventContext,
                                                 promotedSourceInfoWithNoPromoter,
                                                 playlistMetadata);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.LIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.LIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromLikedPromotedPlaylistWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true,
                                                 PLAYLIST_URN,
                                                 eventContext,
                                                 promotedSourceInfo,
                                                 playlistMetadata);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.LIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.LIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().get()).isEqualTo(PROMOTER_URN);
    }

    @Test
    public void shouldCreateEventFromUnlikedTrack() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNLIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNLIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromUnlikedTrackWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNLIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNLIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromUnlikedTrackWithTrackPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(TRACK_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNLIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNLIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromUnlikedPromotedTrack() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false, TRACK_URN, eventContext, promotedSourceInfo, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNLIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNLIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().get()).isEqualTo(PROMOTER_URN);
    }

    @Test
    public void shouldCreateEventFromUnlikedPromotedTrackWithNoPromoter() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false,
                                                 TRACK_URN,
                                                 eventContext,
                                                 promotedSourceInfoWithNoPromoter,
                                                 trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNLIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNLIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromUnlikedPlaylist() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false, PLAYLIST_URN, eventContext, null, playlistMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNLIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNLIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromUnlikedPlaylistWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false, PLAYLIST_URN, eventContext, null, playlistMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNLIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNLIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromUnlikedPromotedPlaylist() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false,
                                                 PLAYLIST_URN,
                                                 eventContext,
                                                 promotedSourceInfo,
                                                 playlistMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNLIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNLIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().get()).isEqualTo(PROMOTER_URN);
    }

    @Test
    public void shouldCreateEventFromUnlikedPromotedPlaylistWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleLike(false,
                                                 PLAYLIST_URN,
                                                 eventContext,
                                                 promotedSourceInfo,
                                                 playlistMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNLIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNLIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().get()).isEqualTo(PROMOTER_URN);
    }

    @Test
    public void shouldCreateEventWithUnknownResourceForUnexpectedUrnType() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent uiEvent = UIEvent.fromToggleLike(true, USER_URN, eventContext, null, EntityMetadata.EMPTY);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.LIKE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.LIKE);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_USERS_2);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromRepostedTrack() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.REPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.REPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();

        assertThat(uiEvent.creatorName().get()).isEqualTo(CREATOR_NAME);
        assertThat(uiEvent.creatorUrn().get()).isEqualTo(USER_URN);
        assertThat(uiEvent.playableTitle().get()).isEqualTo(PLAYABLE_TITLE);
        assertThat(uiEvent.playableUrn().get()).isEqualTo(TRACK_URN);
    }

    @Test
    public void shouldCreateEventFromRepostedTrackFromOverflowMenu() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().isFromOverflow(true).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.REPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.REPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();

        assertThat(uiEvent.creatorName().get()).isEqualTo(CREATOR_NAME);
        assertThat(uiEvent.creatorUrn().get()).isEqualTo(USER_URN);
        assertThat(uiEvent.playableTitle().get()).isEqualTo(PLAYABLE_TITLE);
        assertThat(uiEvent.playableUrn().get()).isEqualTo(TRACK_URN);

        assertThat(uiEvent.isFromOverflow().get()).isTrue();
    }

    @Test
    public void shouldCreateEventFromRepostedTrackFromOverflowWithClickSource() {
        TrackSourceInfo info = new TrackSourceInfo(Screen.STREAM.get(), true);
        info.setSource("stream", "");

        EventContextMetadata eventContext = eventContextNoInvokerScreen().trackSourceInfo(info)
                                                                         .isFromOverflow(true)
                                                                         .build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.REPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.REPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();

        assertThat(uiEvent.creatorName().get()).isEqualTo(CREATOR_NAME);
        assertThat(uiEvent.creatorUrn().get()).isEqualTo(USER_URN);
        assertThat(uiEvent.playableTitle().get()).isEqualTo(PLAYABLE_TITLE);
        assertThat(uiEvent.playableUrn().get()).isEqualTo(TRACK_URN);

        assertThat(uiEvent.isFromOverflow().get()).isTrue();
        assertThat(uiEvent.clickSource().get()).isEqualTo("stream");
    }

    @Test
    public void shouldCreateEventFromRepostedTrackWithTrackPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(TRACK_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.REPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.REPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromRepostedTrackWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.REPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.REPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromRepostedPromotedTrack() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, TRACK_URN, eventContext, promotedSourceInfo, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.REPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.REPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().get()).isEqualTo(PROMOTER_URN);
    }

    @Test
    public void shouldCreateEventFromRepostedPromotedTrackWithNoPromoter() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true,
                                                   TRACK_URN,
                                                   eventContext,
                                                   promotedSourceInfoWithNoPromoter,
                                                   trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.REPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.REPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromRepostedPlaylist() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, PLAYLIST_URN, eventContext, null, playlistMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.REPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.REPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();

        assertThat(uiEvent.creatorName().get()).isEqualTo(CREATOR_NAME);
        assertThat(uiEvent.creatorUrn().get()).isEqualTo(USER_URN);
        assertThat(uiEvent.playableTitle().get()).isEqualTo(PLAYABLE_TITLE);
        assertThat(uiEvent.playableUrn().get()).isEqualTo(PLAYLIST_URN);
    }

    @Test
    public void shouldCreateEventFromRepostedPlaylistWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true, PLAYLIST_URN, eventContext, null, playlistMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.REPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.REPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromRepostedPromotedPlaylist() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true,
                                                   PLAYLIST_URN,
                                                   eventContext,
                                                   promotedSourceInfo,
                                                   playlistMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.REPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.REPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().get()).isEqualTo(PROMOTER_URN);
    }

    @Test
    public void shouldCreateEventFromRepostedPromotedPlaylistWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true,
                                                   PLAYLIST_URN,
                                                   eventContext,
                                                   promotedSourceInfo,
                                                   playlistMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.REPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.REPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().get()).isEqualTo(PROMOTER_URN);
    }

    @Test
    public void shouldCreateEventFromRepostedPromotedPlaylistWithNoPromoter() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(true,
                                                   PLAYLIST_URN,
                                                   eventContext,
                                                   promotedSourceInfoWithNoPromoter,
                                                   playlistMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.REPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.REPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromUnrepostedTrack() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNREPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNREPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromUnrepostedTrackWithTrackPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(TRACK_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNREPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNREPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromUnrepostedTrackWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, TRACK_URN, eventContext, null, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNREPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNREPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromUnrepostedPromotedTrack() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, TRACK_URN, eventContext, promotedSourceInfo, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNREPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNREPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().get()).isEqualTo(PROMOTER_URN);
    }

    @Test
    public void shouldCreateEventFromUnrepostedPromotedTrackWithTrackPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(TRACK_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, TRACK_URN, eventContext, promotedSourceInfo, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNREPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNREPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().get()).isEqualTo(PROMOTER_URN);
    }

    @Test
    public void shouldCreateEventFromUnrepostedPromotedTrackWithNoPromoter() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false,
                                                   TRACK_URN,
                                                   eventContext,
                                                   promotedSourceInfoWithNoPromoter,
                                                   trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNREPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNREPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromUnrepostedPlaylist() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false, PLAYLIST_URN, eventContext, null, playlistMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNREPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNREPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().isPresent()).isFalse();
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromUnrepostedPromotedPlaylist() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false,
                                                   PLAYLIST_URN,
                                                   eventContext,
                                                   promotedSourceInfo,
                                                   playlistMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNREPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNREPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().get()).isEqualTo(PROMOTER_URN);
    }

    @Test
    public void shouldCreateEventFromUnrepostedPromotedPlaylistWithPlaylistPage() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false,
                                                   PLAYLIST_URN,
                                                   eventContext,
                                                   promotedSourceInfo,
                                                   playlistMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNREPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNREPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().get()).isEqualTo(PROMOTER_URN);
    }

    @Test
    public void shouldCreateEventFromUnrepostedPromotedPlaylistWithNoPromoter() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().build();
        UIEvent uiEvent = UIEvent.fromToggleRepost(false,
                                                   PLAYLIST_URN,
                                                   eventContext,
                                                   promotedSourceInfoWithNoPromoter,
                                                   playlistMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.UNREPOST);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.UNREPOST);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(SOUNDCLOUD_PLAYLISTS_42);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);

        assertThat(uiEvent.adUrn().get()).isEqualTo(AD_URN);
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.PROMOTED);
        assertThat(uiEvent.promoterUrn().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromAddToPlaylist() {
        UIEvent uiEvent = UIEvent.fromAddToPlaylist(eventContextBuilder().build());
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.ADD_TO_PLAYLIST);
    }

    @Test
    public void shouldCreateEventFromComment() {
        EventContextMetadata eventContextMetadata = EventContextMetadata.builder().build();
        UIEvent uiEvent = UIEvent.fromComment(trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.COMMENT);
        assertThat(uiEvent.playableTitle().get()).isEqualTo(PLAYABLE_TITLE);
    }

    @Test
    public void shouldCreateEventFromTrackShareRequest() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(TRACK_URN).build();
        UIEvent uiEvent = UIEvent.fromShareRequest(TRACK_URN, eventContext, promotedSourceInfo, trackMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.SHARE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.SHARE_REQUEST);
        assertThat(uiEvent.creatorName().get()).isEqualTo(CREATOR_NAME);
        assertThat(uiEvent.creatorUrn().get()).isEqualTo(USER_URN);
        assertThat(uiEvent.playableTitle().get()).isEqualTo(PLAYABLE_TITLE);
        assertThat(uiEvent.playableUrn().get()).isEqualTo(TRACK_URN);
    }

    @Test
    public void shouldCreateEventFromPlaylistShareRequest() {
        EventContextMetadata eventContext = eventContextNoInvokerScreen().pageUrn(PLAYLIST_URN).build();
        UIEvent uiEvent = UIEvent.fromShareRequest(PLAYLIST_URN, eventContext, promotedSourceInfo, playlistMetadata);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.SHARE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.SHARE_REQUEST);
        assertThat(uiEvent.creatorName().get()).isEqualTo(CREATOR_NAME);
        assertThat(uiEvent.creatorUrn().get()).isEqualTo(USER_URN);
        assertThat(uiEvent.playableTitle().get()).isEqualTo(PLAYABLE_TITLE);
        assertThat(uiEvent.playableUrn().get()).isEqualTo(PLAYLIST_URN);
    }

    @Test
    public void shouldCreateEventFromShuffle() {
        assertThat(UIEvent.fromShuffle(eventContextBuilder().build()).kind()).isEqualTo(UIEvent.Kind.SHUFFLE);
    }

    @Test
    public void shouldCreateEventFromVideoAdFullScreenClick() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(321L));
        UIEvent uiEvent = UIEvent.fromVideoAdFullscreen(videoAd, trackSourceInfo);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.VIDEO_AD_FULLSCREEN);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.VIDEO_AD_FULLSCREEN);
        assertThat(uiEvent.adUrn().get()).isEqualTo(videoAd.getAdUrn().toString());
        assertThat(uiEvent.monetizableTrackUrn().get()).isEqualTo(Urn.forTrack(321));
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.VIDEO);
        assertThat(uiEvent.adTrackingUrls().get()).contains("video_fullscreen1", "video_fullscreen2");
        assertThat(uiEvent.originScreen().get()).isEqualTo("origin screen");
    }

    @Test
    public void shouldCreateEventFromVideoAdShrinkClick() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(321L));
        UIEvent uiEvent = UIEvent.fromVideoAdShrink(videoAd, trackSourceInfo);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.VIDEO_AD_SHRINK);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.VIDEO_AD_SHRINK);
        assertThat(uiEvent.adUrn().get()).isEqualTo(videoAd.getAdUrn().toString());
        assertThat(uiEvent.monetizableTrackUrn().get()).isEqualTo(Urn.forTrack(321));
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.VIDEO);
        assertThat(uiEvent.adTrackingUrls().get()).contains("video_exit_full1", "video_exit_full2");
        assertThat(uiEvent.originScreen().get()).isEqualTo("origin screen");
    }

    @Test
    public void shouldCreateEventFromVideoAdMuteClick() {
        VideoAd videoAd = AdFixtures.getInlayVideoAd(321L);
        UIEvent uiEvent = UIEvent.fromVideoMute(videoAd, trackSourceInfo);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.VIDEO_AD_MUTE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.VIDEO_AD_MUTE);
        assertThat(uiEvent.adUrn().get()).isEqualTo(videoAd.getAdUrn().toString());
        assertThat(uiEvent.monetizableTrackUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.INLAY);
        assertThat(uiEvent.adTrackingUrls().get()).contains("video_mute1", "video_mute2");
        assertThat(uiEvent.originScreen().get()).isEqualTo("origin screen");
    }

    @Test
    public void shouldCreateEventFromVideoAdUnmuteClick() {
        VideoAd videoAd = AdFixtures.getInlayVideoAd(321L);
        UIEvent uiEvent = UIEvent.fromVideoUnmute(videoAd, trackSourceInfo);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.VIDEO_AD_UNMUTE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.VIDEO_AD_UNMUTE);
        assertThat(uiEvent.adUrn().get()).isEqualTo(videoAd.getAdUrn().toString());
        assertThat(uiEvent.monetizableTrackUrn().isPresent()).isFalse();
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.INLAY);
        assertThat(uiEvent.adTrackingUrls().get()).contains("video_unmute1", "video_unmute2");
        assertThat(uiEvent.originScreen().get()).isEqualTo("origin screen");
    }

    @Test
    public void shouldCreateEventFromVideoAdClickThrough() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(321L));
        UIEvent uiEvent = UIEvent.fromPlayableClickThrough(videoAd, trackSourceInfo);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.AD_CLICKTHROUGH);
        assertThat(uiEvent.adUrn().get()).isEqualTo(videoAd.getAdUrn().toString());
        assertThat(uiEvent.monetizableTrackUrn().get()).isEqualTo(Urn.forTrack(321));
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.VIDEO);
        assertThat(uiEvent.clickthroughsUrl().get()).isEqualTo("http://clickthrough.videoad.com");
        assertThat(uiEvent.clickthroughsKind().get()).isEqualTo("clickthrough::video_ad");
        assertThat(uiEvent.adTrackingUrls().get()).contains("video_click1", "video_click2");
        assertThat(uiEvent.originScreen().get()).isEqualTo("origin screen");
    }

    @Test
    public void shouldCreateEventFromAudioAdClickThrough() {
        AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(321L));
        UIEvent uiEvent = UIEvent.fromPlayableClickThrough(audioAd, trackSourceInfo);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.AD_CLICKTHROUGH);
        assertThat(uiEvent.adUrn().get()).isEqualTo(audioAd.getAdUrn().toString());
        assertThat(uiEvent.monetizableTrackUrn().get()).isEqualTo(Urn.forTrack(321));
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.AUDIO);
        assertThat(uiEvent.clickthroughsUrl().get()).isEqualTo("http://clickthrough.visualad.com");
        assertThat(uiEvent.clickthroughsKind().get()).isEqualTo("clickthrough::audio_ad");
        assertThat(uiEvent.adTrackingUrls().get()).contains("comp_click1", "comp_click2");
        assertThat(uiEvent.originScreen().get()).isEqualTo("origin screen");
    }

    @Test
    public void shouldCreateEventFromAppInstallClickThrough() {
        AppInstallAd appInstall = AppInstallAd.create(AdFixtures.getApiAppInstall());
        UIEvent uiEvent = UIEvent.fromAppInstallAdClickThrough(appInstall);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.AD_CLICKTHROUGH);
        assertThat(uiEvent.adUrn().get()).isEqualTo(appInstall.getAdUrn().toString());
        assertThat(uiEvent.monetizationType().get().key()).isEqualTo("mobile_inlay");
        assertThat(uiEvent.clickthroughsUrl().get()).isEqualTo("http://clickthrough.com");
        assertThat(uiEvent.clickthroughsKind().get()).isEqualTo("clickthrough::app_install");
        assertThat(uiEvent.adTrackingUrls().get()).contains("app_click1", "app_click2");
    }

    @Test
    public void shouldCreateEventFromVideoAdSkip() {
        VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(321L));
        UIEvent uiEvent = UIEvent.fromSkipAdClick(videoAd, trackSourceInfo);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.SKIP_AD_CLICK);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.SKIP_AD_CLICK);
        assertThat(uiEvent.adUrn().get()).isEqualTo(videoAd.getAdUrn().toString());
        assertThat(uiEvent.monetizableTrackUrn().get()).isEqualTo(Urn.forTrack(321));
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.VIDEO);
        assertThat(uiEvent.adTrackingUrls().get()).contains("video_skip1", "video_skip2");
        assertThat(uiEvent.originScreen().get()).isEqualTo("origin screen");
    }

    @Test
    public void shouldCreateEventFromAudioAdSkip() {
        AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(321L));
        UIEvent uiEvent = UIEvent.fromSkipAdClick(audioAd, trackSourceInfo);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.SKIP_AD_CLICK);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.SKIP_AD_CLICK);
        assertThat(uiEvent.adUrn().get()).isEqualTo(audioAd.getAdUrn().toString());
        assertThat(uiEvent.monetizableTrackUrn().get()).isEqualTo(Urn.forTrack(321));
        assertThat(uiEvent.monetizationType().get()).isEqualTo(MonetizationType.AUDIO);
        assertThat(uiEvent.adTrackingUrls().get()).contains("audio_skip1", "audio_skip2");
        assertThat(uiEvent.originScreen().get()).isEqualTo("origin screen");
    }

    @Test
    public void shouldCreateEventFromCreatePlaylist() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        UIEvent event = UIEvent.fromCreatePlaylist(EntityMetadata.from(playlist));

        assertThat(event.kind()).isEqualTo(UIEvent.Kind.CREATE_PLAYLIST);
        assertThat(event.playableTitle().get()).isEqualTo(playlist.getTitle());
        assertThat(event.playableUrn().get()).isEqualTo(playlist.getUrn());
    }

    @Test
    public void shouldCreateEventFromStartStation() {
        UIEvent event = UIEvent.fromStartStation();

        assertThat(event.kind()).isEqualTo(UIEvent.Kind.START_STATION);
    }

    @Test
    public void shouldCreateEventFromNavigation() {
        final AttributingActivity attributingActivity = AttributingActivity.create(AttributingActivity.POSTED,
                                                                                   Optional.absent());

        EventContextMetadata eventContext = eventContextNoInvokerScreen()
                .attributingActivity(attributingActivity)
                .linkType(LinkType.OWNER)
                .build();

        UIEvent uiEvent = UIEvent.fromNavigation(TRACK_URN, eventContext);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.NAVIGATION);

        assertThat(uiEvent.pageUrn().get()).isEqualTo(PAGE_URN);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(CLICK_OBJECT_URN);
        assertThat(uiEvent.originScreen().get()).isEqualTo(ORIGIN_SCREEN_NAME);
        assertThat(uiEvent.attributingActivity().isPresent()).isTrue();
        assertThat(uiEvent.attributingActivity().get()).isEqualTo(attributingActivity);
        assertThat(uiEvent.linkType().get()).isEqualTo(LinkType.OWNER.getName());
    }

    @Test
    public void shouldHaveQueryUrnWhenHasStationsSourceInfo() {
        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.STATIONS_INFO.get(), false);
        final EventContextMetadata eventContextMetadata = EventContextMetadata.builder().trackSourceInfo(trackSourceInfo).build();

        trackSourceInfo.setStationSourceInfo(Urn.forArtistStation(123L), StationsSourceInfo.create(QUERY_URN));

        final UIEvent event = UIEvent.fromToggleLike(true, Urn.NOT_SET, eventContextMetadata, null, EntityMetadata.EMPTY);
        assertThat(event.queryUrn()).isEqualTo(Optional.of(QUERY_URN));
    }

    @Test
    public void shouldHaveQueryUrnAndPositionWhenHasQuerySourceInfo() {
        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.UNKNOWN.get(), false);
        final EventContextMetadata eventContextMetadata = EventContextMetadata.builder().trackSourceInfo(trackSourceInfo).build();

        trackSourceInfo.setQuerySourceInfo(QuerySourceInfo.create(QUERY_POSITION, QUERY_URN));

        final UIEvent event = UIEvent.fromToggleLike(true, Urn.NOT_SET, eventContextMetadata, null, EntityMetadata.EMPTY);
        assertThat(event.queryUrn()).isEqualTo(Optional.of(QUERY_URN));
        assertThat(event.queryPosition()).isEqualTo(Optional.of(QUERY_POSITION));
    }

    @Test
    public void shouldHaveAbsentQueryUrnAndPositionIfNoReleventSourceInfo() {
        final EventContextMetadata eventContextMetadata = EventContextMetadata.builder().build();


        final UIEvent event = UIEvent.fromToggleLike(true, Urn.NOT_SET, eventContextMetadata, null, EntityMetadata.EMPTY);
        assertThat(event.queryUrn()).isEqualTo(Optional.<TrackSourceInfo>absent());
        assertThat(event.queryPosition()).isEqualTo(Optional.<Integer>absent());
    }

    @Test
    public void shouldCreateEventFromPlayQueueShuffleOn() {
        final UIEvent uiEvent = UIEvent.fromPlayQueueShuffle(true);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_SHUFFLE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.SHUFFLE_ON);
        assertThat(uiEvent.originScreen().get()).isEqualTo(Screen.PLAY_QUEUE.get());
    }

    @Test
    public void shouldCreateEventFromPlayQueueShuffleOff() {
        final UIEvent uiEvent = UIEvent.fromPlayQueueShuffle(false);
        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_SHUFFLE);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.SHUFFLE_OFF);
        assertThat(uiEvent.originScreen().get()).isEqualTo(Screen.PLAY_QUEUE.get());
    }

    @Test
    public void shouldCreateEventFromPlayQueueRepeat() {
        final UIEvent uiEvent = UIEvent.fromPlayQueueRepeat(Screen.PLAY_QUEUE, RepeatMode.REPEAT_ALL);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_REPEAT);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.PLAY_QUEUE_REPEAT_ON);
        assertThat(uiEvent.originScreen().get()).isEqualTo(Screen.PLAY_QUEUE.get());
        assertThat(uiEvent.playQueueRepeatMode().get()).isEqualTo(RepeatMode.REPEAT_ALL.get());
    }

    @Test
    public void shouldCreateEventFromPlayQueueRepeatOff() {
        final UIEvent uiEvent = UIEvent.fromPlayQueueRepeat(Screen.PLAY_QUEUE, RepeatMode.REPEAT_NONE);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_REPEAT);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.PLAY_QUEUE_REPEAT_OFF);
        assertThat(uiEvent.originScreen().get()).isEqualTo(Screen.PLAY_QUEUE.get());
        assertThat(uiEvent.playQueueRepeatMode().isPresent()).isFalse();
    }

    @Test
    public void shouldCreateEventFromPlayNext() {
        final EventContextMetadata eventContextMetadata = EventContextMetadata.builder().build();
        final String screen = Screen.STREAM.get();
        final Urn urn = Urn.forTrack(123L);

        final UIEvent uiEvent = UIEvent.fromPlayNext(urn, screen, eventContextMetadata);

        assertThat(uiEvent.kind()).isEqualTo(UIEvent.Kind.PLAY_NEXT);
        assertThat(uiEvent.clickName().get()).isEqualTo(UIEvent.ClickName.PLAY_NEXT);
        assertThat(uiEvent.clickObjectUrn().get()).isEqualTo(urn);
        assertThat(uiEvent.originScreen().get()).isEqualTo(screen);
    }

    private EventContextMetadata.Builder eventContextBuilder() {
        return eventContextNoInvokerScreen();
    }

    private EventContextMetadata.Builder eventContextNoInvokerScreen() {
        return EventContextMetadata.builder()
                                   .pageName(ORIGIN_SCREEN_NAME)
                                   .pageUrn(Urn.NOT_SET);
    }

    private EntityMetadata buildMetadata(Urn urn) {
        return EntityMetadata.from(CREATOR_NAME, USER_URN, PLAYABLE_TITLE, urn);
    }

    private EntityMetadata buildUserMetadata(Urn urn) {
        return EntityMetadata.from(CREATOR_NAME, urn, Strings.EMPTY, Urn.NOT_SET);
    }

}
