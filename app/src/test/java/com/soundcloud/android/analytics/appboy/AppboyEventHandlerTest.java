package com.soundcloud.android.analytics.appboy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.appboy.models.outgoing.AppboyProperties;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.PlayableMetadata;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.explore.ExploreGenre;
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

    private static final PropertySet trackPropertySet = TestPropertySets.expectedTrackForPlayer();
    private static final TrackItem track = TrackItem.from(trackPropertySet);

    private static final AppboyProperties playableOnlyProperties = new AppboyProperties()
            .addProperty("creator_display_name", track.getCreatorName())
            .addProperty("creator_urn", track.getCreatorUrn().toString())
            .addProperty("playable_title", track.getTitle())
            .addProperty("playable_urn", track.getEntityUrn().toString())
            .addProperty("playable_type", "track");

    private static final PlayableMetadata metadata = PlayableMetadata.from(trackPropertySet);

    private AppboyEventHandler eventHandler;

    @Before
    public void setUp() throws Exception {
        eventHandler = new AppboyEventHandler(appboy);
    }

    @Test
    public void shouldTrackLikeEvents() {
        UIEvent event = UIEvent.fromToggleLike(true, "invoker_screen", "context_screen", "page_name",
                Urn.forTrack(123), Urn.NOT_SET, null, metadata);

        eventHandler.handleEvent(event);

        expectCustomEvent("like", playableOnlyProperties);
    }

    @Test
    public void shouldNotTrackUnLikeEvents() {
        UIEvent event = UIEvent.fromToggleLike(false, "invoker_screen", "context_screen", "page_name",
                Urn.forTrack(123), Urn.NOT_SET, null, metadata);

        eventHandler.handleEvent(event);

        verify(appboy, never()).logCustomEvent(any(String.class), any(AppboyProperties.class));
    }

    @Test
    public void shouldTrackUserTriggeredPlaySessionEvents() {
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo("origin", true);
        PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(trackPropertySet, Urn.forUser(123L),
                trackSourceInfo, 0l, 10000l, "https", "player", "wifi", false);

        eventHandler.handleEvent(event);

        expectCustomEvent("play", playableOnlyProperties);
    }

    @Test
    public void shouldTriggerImmediateFlushOnManualTriggerPlay() {
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo("origin", true);
        PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(trackPropertySet, Urn.forUser(123L),
                trackSourceInfo, 0l, 10000l, "https", "player", "wifi", false);

        eventHandler.handleEvent(event);

        verify(appboy).requestImmediateDataFlush();
    }

    @Test
    public void shouldNotTrackAutomaticTriggeredPlaySessionEvents() {
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo("origin", false);
        PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(trackPropertySet, Urn.forUser(123L),
                trackSourceInfo, 0l, 10000l, "https", "player", "wifi", false);

        eventHandler.handleEvent(event);

        verify(appboy, never()).logCustomEvent(any(String.class), any(AppboyProperties.class));
    }

    @Test
    public void shouldNotTrackPauseEvents() {
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo("origin", false);

        PlaybackSessionEvent previousEvent = PlaybackSessionEvent.forPlay(trackPropertySet, Urn.forUser(123L),
                trackSourceInfo, 0l, 10000l, "https", "player", "wifi", false);

        PlaybackSessionEvent event = PlaybackSessionEvent.forStop(trackPropertySet, Urn.forUser(123L),
                trackSourceInfo, previousEvent, 0l, 10000l, "https", "player", "wifi",
                PlaybackSessionEvent.STOP_REASON_PAUSE, false);

        eventHandler.handleEvent(event);

        verify(appboy, never()).logCustomEvent(any(String.class), any(AppboyProperties.class));
    }

    @Test
    public void shouldTrackCommentEvents() {
        UIEvent event = UIEvent.fromComment("screen", 123l, metadata);

        eventHandler.handleEvent(event);

        expectCustomEvent("comment", playableOnlyProperties);
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
        UIEvent event = UIEvent.fromShare("screen", Urn.forTrack(123l), metadata);

        eventHandler.handleEvent(event);

        expectCustomEvent("share", playableOnlyProperties);
    }

    private void expectCustomEvent(String eventName, AppboyProperties expectedProperties) {
        ArgumentCaptor<AppboyProperties> captor = ArgumentCaptor.forClass(AppboyProperties.class);

        verify(appboy).logCustomEvent(eq(eventName), captor.capture());

        String generatedJson = captor.getValue().forJsonPut().toString();
        String expectedJson = expectedProperties.forJsonPut().toString();

        assertThat(generatedJson).isEqualTo(expectedJson);
    }

}
