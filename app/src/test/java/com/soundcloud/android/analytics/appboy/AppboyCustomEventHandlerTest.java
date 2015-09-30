package com.soundcloud.android.analytics.appboy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.appboy.models.outgoing.AppboyProperties;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.UIEvent;
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

public class AppboyCustomEventHandlerTest extends AndroidUnitTest {

    @Mock private AppboyWrapper appboy;

    private static final PropertySet trackPropertySet = TestPropertySets.expectedTrackForPlayer();
    private static final TrackItem track = TrackItem.from(trackPropertySet);
    private AppboyCustomEventHandler eventHandler;

    @Before
    public void setUp() throws Exception {
        eventHandler = new AppboyCustomEventHandler(appboy);
    }

    @Test
    public void shouldTrackLikeEvents() {
        UIEvent event = UIEvent.fromToggleLike(true, "invoker_screen", "context_screen", "page_name",
                Urn.forTrack(123), Urn.NOT_SET, null, track);

        AppboyProperties expectedProperties = new AppboyProperties()
                .addProperty("creator_display_name", track.getCreatorName())
                .addProperty("creator_urn", track.getCreatorUrn().toString())
                .addProperty("playable_title", track.getTitle())
                .addProperty("playable_urn", track.getEntityUrn().toString())
                .addProperty("playable_type", track.getPlayableType());

        eventHandler.handleEvent(event);

        expectCustomEvent("like", expectedProperties);
    }

    @Test
    public void shouldNotTrackUnLikeEvents() {
        UIEvent event = UIEvent.fromToggleLike(false, "invoker_screen", "context_screen", "page_name",
                Urn.forTrack(123), Urn.NOT_SET, null, track);

        eventHandler.handleEvent(event);

        verify(appboy, never()).logCustomEvent(any(String.class), any(AppboyProperties.class));
    }

    @Test
    public void shouldTrackUserTriggeredPlaySessionEvents() {
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo("origin", true);
        PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(trackPropertySet, Urn.forUser(123L),
                trackSourceInfo, 0l, 10000l, "https", "player", "wifi", false);

        AppboyProperties expectedProperties = new AppboyProperties()
                .addProperty("creator_display_name", track.getCreatorName())
                .addProperty("creator_urn", track.getCreatorUrn().toString())
                .addProperty("playable_title", track.getTitle())
                .addProperty("playable_urn", track.getEntityUrn().toString())
                .addProperty("playable_type", track.getPlayableType());

        eventHandler.handleEvent(event);

        expectCustomEvent("play", expectedProperties);
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

    private void expectCustomEvent(String eventName, AppboyProperties expectedProperties) {
        ArgumentCaptor<AppboyProperties> captor = ArgumentCaptor.forClass(AppboyProperties.class);

        verify(appboy).logCustomEvent(eq(eventName), captor.capture());

        String generatedJson = captor.getValue().forJsonPut().toString();
        String expectedJson = expectedProperties.forJsonPut().toString();

        assertThat(generatedJson).isEqualTo(expectedJson);
    }

}