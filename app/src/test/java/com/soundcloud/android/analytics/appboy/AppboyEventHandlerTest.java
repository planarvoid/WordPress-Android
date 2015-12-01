package com.soundcloud.android.analytics.appboy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.appboy.models.outgoing.AppboyProperties;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.AttributionEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.explore.ExploreGenre;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class AppboyEventHandlerTest extends AndroidUnitTest {

    @Mock private AppboyWrapper appboy;
    @Mock private AppboyPlaySessionState appboyPlaySessionState;

    private static final PropertySet TRACK_PROPERTY_SET = TestPropertySets.expectedTrackForPlayer();
    private static final TrackItem TRACK = TrackItem.from(TRACK_PROPERTY_SET);
    private static final TrackSourceInfo TRACK_SOURCE_INFO = new TrackSourceInfo("origin", true);
    private static final PlaybackSessionEvent MARKETABLE_PLAY_EVENT = PlaybackSessionEvent.forPlay(TRACK_PROPERTY_SET, Urn.forUser(123L), TRACK_SOURCE_INFO, 0l, 10000l, "https", "player", "wifi", false, true);
    private static final PlaybackSessionEvent NON_MARKETABLE_PLAY_EVENT = PlaybackSessionEvent.forPlay(TRACK_PROPERTY_SET, Urn.forUser(123L), TRACK_SOURCE_INFO, 0l, 10000l, "https", "player", "wifi", false, false);

    private static final AppboyProperties PLAYABLE_ONLY_PROPERTIES = new AppboyProperties()
            .addProperty("creator_display_name", TRACK.getCreatorName())
            .addProperty("creator_urn", TRACK.getCreatorUrn().toString())
            .addProperty("playable_title", TRACK.getTitle())
            .addProperty("playable_urn", TRACK.getEntityUrn().toString())
            .addProperty("playable_type", "track");


    private static final EntityMetadata METADATA = EntityMetadata.from(TRACK_PROPERTY_SET);

    private AppboyEventHandler eventHandler;

    @Before
    public void setUp() throws Exception {
        eventHandler = new AppboyEventHandler(appboy, appboyPlaySessionState);
        when(appboyPlaySessionState.isSessionPlayed()).thenReturn(false);
    }

    @Test
    public void shouldTrackLikeEvents() {
        EventContextMetadata eventContext = eventContextBuilder().invokerScreen("invoker_screen").build();
        UIEvent event = UIEvent.fromToggleLike(true, Urn.forTrack(123), eventContext, null, METADATA);

        eventHandler.handleEvent(event);

        expectCustomEvent("like", PLAYABLE_ONLY_PROPERTIES);
    }

    @Test
    public void shouldNotTrackUnLikeEvents() {
        EventContextMetadata eventContext = eventContextBuilder().invokerScreen("invoker_screen").build();
        UIEvent event = UIEvent.fromToggleLike(false, Urn.forTrack(123), eventContext, null, METADATA);

        eventHandler.handleEvent(event);

        verify(appboy, never()).logCustomEvent(any(String.class), any(AppboyProperties.class));
    }


    @Test
    public void shouldTrackMarketablePlaySessionEvents() {
        eventHandler.handleEvent(MARKETABLE_PLAY_EVENT);

        expectCustomEvent("play", PLAYABLE_ONLY_PROPERTIES);
    }

    @Test
    public void shouldTriggerImmediateFlushOnMarketablePlays() {
        eventHandler.handleEvent(MARKETABLE_PLAY_EVENT);

        verify(appboy).requestImmediateDataFlush();
    }

    @Test
    public void shouldTriggerInAppMessagesRefresh() {
        eventHandler.handleEvent(MARKETABLE_PLAY_EVENT);

        verify(appboy).requestInAppMessageRefresh();
    }

    @Test
    public void shouldNotTrackNonMarketablePlays() {
        eventHandler.handleEvent(NON_MARKETABLE_PLAY_EVENT);

        verify(appboy, never()).logCustomEvent(any(String.class), any(AppboyProperties.class));
    }

    @Test
    public void shouldNotTrackWhenSessionAlreadyPlayed() {
        when(appboyPlaySessionState.isSessionPlayed()).thenReturn(true);

        eventHandler.handleEvent(MARKETABLE_PLAY_EVENT);

        verify(appboy, never()).logCustomEvent(any(String.class), any(AppboyProperties.class));
    }

    @Test
    public void shouldNotTrackPauseEvents() {
        PlaybackSessionEvent event = PlaybackSessionEvent.forStop(TRACK_PROPERTY_SET, Urn.forUser(123L),
                TRACK_SOURCE_INFO, NON_MARKETABLE_PLAY_EVENT, 0l, 10000l, "https", "player", "wifi",
                PlaybackSessionEvent.STOP_REASON_PAUSE, false);

        eventHandler.handleEvent(event);

        verify(appboy, never()).logCustomEvent(any(String.class), any(AppboyProperties.class));
    }

    @Test
    public void shouldTrackCommentEvents() {
        EventContextMetadata eventContextMetadata = EventContextMetadata.builder().contextScreen("screen").build();
        UIEvent event = UIEvent.fromComment(eventContextMetadata, 123l, METADATA);

        eventHandler.handleEvent(event);

        expectCustomEvent("comment", PLAYABLE_ONLY_PROPERTIES);
    }

    @Test
    public void shouldTrackExploreGenresScreens() {
        ScreenEvent event = ScreenEvent.create(Screen.EXPLORE_MUSIC_GENRE.get(), ExploreGenre.POPULAR_AUDIO_CATEGORY);
        AppboyProperties properties = new AppboyProperties();
        properties.addProperty("genre", ExploreGenre.POPULAR_AUDIO_CATEGORY.getTitle());
        properties.addProperty("category", event.getScreenTag());

        eventHandler.handleEvent(event);

        expectCustomEvent("explore", properties);
    }

    @Test
    public void exploreTrendingAudioOrMusicTrackingShouldOnlyContainCategory() {
        ScreenEvent event = ScreenEvent.create(Screen.EXPLORE_TRENDING_AUDIO);
        AppboyProperties properties = new AppboyProperties();
        properties.addProperty("category", event.getScreenTag());

        eventHandler.handleEvent(event);

        expectCustomEvent("explore", properties);
    }

    @Test
    public void shouldTrackSearchEvents() {
        SearchEvent event = SearchEvent.searchStart(Screen.SEARCH_EVERYTHING, null);

        eventHandler.handleEvent(event);

        verify(appboy).logCustomEvent("search");
    }

    @Test
    public void shouldTrackShareEvents() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(Urn.forTrack(123L)).build();
        UIEvent event = UIEvent.fromShare(Urn.forTrack(123l), eventContext, null, METADATA);

        eventHandler.handleEvent(event);

        expectCustomEvent("share", PLAYABLE_ONLY_PROPERTIES);
    }

    @Test
    public void shouldTrackAttributionEvents() {
        AttributionEvent event = new AttributionEvent("net", "cam", "adg", "cre");

        eventHandler.handleEvent(event);

        verify(appboy).setAttribution("net", "cam", "adg", "cre");
    }

    @Test
    public void shouldTrackRepostEvents() {
        UIEvent event = UIEvent.fromToggleRepost(true, Urn.forTrack(123), eventContextBuilder().build(), null, METADATA);

        eventHandler.handleEvent(event);

        expectCustomEvent("repost", PLAYABLE_ONLY_PROPERTIES);
    }

    @Test
    public void shouldNotTrackUnRepostEvents() {
        EventContextMetadata eventContext = eventContextBuilder().build();
        UIEvent event = UIEvent.fromToggleRepost(false, Urn.forTrack(123), eventContext, null, METADATA);

        eventHandler.handleEvent(event);

        verify(appboy, never()).logCustomEvent(any(String.class), any(AppboyProperties.class));
    }

    @Test
    public void shouldTrackPlaylistCreation() {
        PropertySet playlist = TestPropertySets.expectedPostedPlaylistForPostsScreen();
        UIEvent event = UIEvent.fromCreatePlaylist(EntityMetadata.from(playlist));
        AppboyProperties expectedProperties = new AppboyProperties()
                .addProperty("playlist_title", playlist.get(PlayableProperty.TITLE))
                .addProperty("playlist_urn", playlist.get(PlayableProperty.URN).toString());

        eventHandler.handleEvent(event);

        expectCustomEvent("create_playlist", expectedProperties);
    }

    private void expectCustomEvent(String eventName, AppboyProperties expectedProperties) {
        ArgumentCaptor<AppboyProperties> captor = ArgumentCaptor.forClass(AppboyProperties.class);

        verify(appboy).logCustomEvent(eq(eventName), captor.capture());

        String generatedJson = captor.getValue().forJsonPut().toString();
        String expectedJson = expectedProperties.forJsonPut().toString();

        assertThat(generatedJson).isEqualTo(expectedJson);
    }

    private EventContextMetadata.Builder eventContextBuilder() {
        return EventContextMetadata.builder()
                .contextScreen("context_screen")
                .pageName("page_name")
                .pageUrn(Urn.NOT_SET);
    }
}
