package com.soundcloud.android.analytics.appboy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.appboy.models.outgoing.AppboyProperties;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.AttributionEvent;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlaybackSessionEventArgs;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.support.annotation.NonNull;

public class AppboyEventHandlerTest extends AndroidUnitTest {

    @Mock private AppboyWrapper appboy;
    @Mock private AppboyPlaySessionState appboyPlaySessionState;

    private static final TrackItem TRACK = TestPropertySets.expectedTrackForPlayer();
    private static final TrackSourceInfo TRACK_SOURCE_INFO = new TrackSourceInfo("origin", true);
    private static final String UUID = "uuid";
    private static final String PLAY_ID = "play_id";

    private static final PlaybackSessionEvent MARKETABLE_PLAY_EVENT = PlaybackSessionEvent.forPlay(
            PlaybackSessionEventArgs.create(TRACK, TRACK_SOURCE_INFO, 0L, "https",
                                            "player", false, true, UUID, PLAY_ID));

    private static final PlaybackSessionEvent NON_MARKETABLE_PLAY_EVENT = PlaybackSessionEvent.forPlay(
            PlaybackSessionEventArgs.create(TRACK, TRACK_SOURCE_INFO, 0L, "https",
                                            "player", false, false, UUID, PLAY_ID));

    private static final AppboyProperties PLAYABLE_ONLY_PROPERTIES = basePlayableProperties();

    private static final AppboyProperties PLAYBACK_PROPERTIES = basePlayableProperties()
            .addProperty("monetization_model", "monetization-model");

    private static final EntityMetadata METADATA = EntityMetadata.from(TRACK);

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

        expectCustomEvent("play", PLAYBACK_PROPERTIES);
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
        final PlaybackSessionEventArgs args = PlaybackSessionEventArgs.create(TRACK,
                                                                              TRACK_SOURCE_INFO,
                                                                              0L,
                                                                              "https",
                                                                              "player",
                                                                              false,
                                                                              false,
                                                                              UUID,
                                                                              PLAY_ID);
        PlaybackSessionEvent event = PlaybackSessionEvent.forStop(NON_MARKETABLE_PLAY_EVENT,
                                                                  PlaybackSessionEvent.STOP_REASON_PAUSE,
                                                                  args);

        eventHandler.handleEvent(event);

        verify(appboy, never()).logCustomEvent(any(String.class), any(AppboyProperties.class));
    }

    @Test
    public void shouldTrackCommentEvents() {
        EventContextMetadata eventContextMetadata = EventContextMetadata.builder().contextScreen("screen").build();
        UIEvent event = UIEvent.fromComment(eventContextMetadata, METADATA);

        eventHandler.handleEvent(event);

        expectCustomEvent("comment", PLAYABLE_ONLY_PROPERTIES);
    }

    @Test
    public void shouldTrackSearchEvents() {
        SearchEvent event = SearchEvent.searchStart(Screen.SEARCH_EVERYTHING,
                                                    new SearchQuerySourceInfo(new Urn("soundcloud:query:123"),
                                                                                    "query"));

        eventHandler.handleEvent(event);

        verify(appboy).logCustomEvent("search");
    }

    @Test
    public void shouldTrackShareEvents() {
        EventContextMetadata eventContext = eventContextBuilder().pageUrn(Urn.forTrack(123L)).build();
        UIEvent event = UIEvent.fromShare(Urn.forTrack(123L), eventContext, null, METADATA);

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
        UIEvent event = UIEvent.fromToggleRepost(true,
                                                 Urn.forTrack(123),
                                                 eventContextBuilder().build(),
                                                 null,
                                                 METADATA);

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
        PlaylistItem playlist = TestPropertySets.expectedPostedPlaylistForPostsScreen();
        UIEvent event = UIEvent.fromCreatePlaylist(EntityMetadata.from(playlist));
        AppboyProperties expectedProperties = new AppboyProperties()
                .addProperty("playlist_title", playlist.getTitle())
                .addProperty("playlist_urn", playlist.getUrn().toString());

        eventHandler.handleEvent(event);

        expectCustomEvent("create_playlist", expectedProperties);
    }

    @Test
    public void shouldTrackStartStation() {
        UIEvent event = UIEvent.fromStartStation();

        eventHandler.handleEvent(event);

        verify(appboy).logCustomEvent("start_station");
    }

    @Test
    public void shouldTrackOfflineLikesEnabled() {
        OfflineInteractionEvent event = OfflineInteractionEvent.fromEnableOfflineLikes("ignored");

        eventHandler.handleEvent(event);

        expectCustomEvent("offline_content", enabled("likes"));
    }

    @Test
    public void shouldTrackOfflineLikesDisabled() {
        OfflineInteractionEvent event = OfflineInteractionEvent.fromRemoveOfflineLikes("ignored");

        eventHandler.handleEvent(event);

        expectCustomEvent("offline_content", disabled("likes"));
    }

    @Test
    public void shouldTrackOfflinePlaylistEnabled() {
        OfflineInteractionEvent event = OfflineInteractionEvent.fromAddOfflinePlaylist("ignored",
                                                                                       Urn.forPlaylist(123L),
                                                                                       null);

        eventHandler.handleEvent(event);

        expectCustomEvent("offline_content", enabled("playlist"));
    }

    @Test
    public void shouldTrackOfflinePlaylistDisabled() {
        OfflineInteractionEvent event = OfflineInteractionEvent.fromRemoveOfflinePlaylist("ignored",
                                                                                          Urn.forPlaylist(123L),
                                                                                          null);

        eventHandler.handleEvent(event);

        expectCustomEvent("offline_content", disabled("playlist"));
    }

    @Test
    public void shouldTrackOfflineCollectionEnabled() {
        OfflineInteractionEvent event = OfflineInteractionEvent.fromEnableCollectionSync("ignored");

        eventHandler.handleEvent(event);

        expectCustomEvent("offline_content", enabled("all"));
    }

    @Test
    public void shouldTrackOfflineCollectionDisabled() {
        OfflineInteractionEvent event = OfflineInteractionEvent.fromDisableCollectionSync("ignored");

        eventHandler.handleEvent(event);

        expectCustomEvent("offline_content", disabled("all"));
    }

    private AppboyProperties enabled(String context) {
        return new AppboyProperties()
                .addProperty("context", context)
                .addProperty("enabled", true);
    }

    private AppboyProperties disabled(String context) {
        return new AppboyProperties()
                .addProperty("context", context)
                .addProperty("enabled", false);
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

    @NonNull
    private static AppboyProperties basePlayableProperties() {
        return new AppboyProperties()
                .addProperty("creator_display_name", TRACK.getCreatorName())
                .addProperty("creator_urn", TRACK.getCreatorUrn().toString())
                .addProperty("playable_title", TRACK.getTitle())
                .addProperty("playable_urn", TRACK.getUrn().toString())
                .addProperty("playable_type", "track");
    }

}
