package com.soundcloud.android.analytics;

import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedPromotedTrack;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForPlayer;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForWidget;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

public class EngagementsTrackingTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123l);
    private static final PropertySet promotedTrack = expectedPromotedTrack();
    private static final PropertySet playerTrack = expectedTrackForPlayer();
    private static final PropertySet widgetTrack = expectedTrackForWidget();

    private final TestEventBus eventBus = new TestEventBus();

    private EngagementsTracking engagementsTracking;

    @Mock private TrackRepository trackRepository;


    @Before
    public void setUp() {
        engagementsTracking = new EngagementsTracking(eventBus, trackRepository);
    }

    @Test
    public void testLikeTrackUrnForPromotedTrack() {
        final PromotedTrackItem trackItem = PromotedTrackItem.from(promotedTrack);
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(trackItem);
        final PlayableMetadata playableMetadata = PlayableMetadata.from(promotedTrack);

        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(promotedTrack));

        engagementsTracking.likeTrackUrn(TRACK_URN, true, "invoker", "context_screen", "page", TRACK_URN, promotedSourceInfo);

        UIEvent expectedEvent = UIEvent.fromToggleLike(true,
                "invoker",
                "context_screen",
                "page",
                TRACK_URN,
                TRACK_URN,
                promotedSourceInfo,
                playableMetadata);

        UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(expectedEvent.getKind());
        assertThat(event.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

    @Test
    public void testLikeTrackUrnForPlayerTrack() {
        final PlayableMetadata playableMetadata = PlayableMetadata.from(playerTrack);

        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(playerTrack));

        engagementsTracking.likeTrackUrn(TRACK_URN, true, "invoker", "context_screen", "page", TRACK_URN, null);

        UIEvent expectedEvent = UIEvent.fromToggleLike(true,
                "invoker",
                "context_screen",
                "page",
                TRACK_URN,
                TRACK_URN,
                null,
                playableMetadata);

        UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(expectedEvent.getKind());
        assertThat(event.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

    @Test
    public void testLikeTrackUrnForWidgetTrack() {
        final PlayableMetadata playableMetadata = PlayableMetadata.from(widgetTrack);

        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(widgetTrack));

        engagementsTracking.likeTrackUrn(TRACK_URN, true, "widget", "context_screen", "widget", Urn.NOT_SET, null);

        UIEvent expectedEvent = UIEvent.fromToggleLike(true,
                "widget",
                "context_screen",
                "widget",
                TRACK_URN,
                Urn.NOT_SET,
                null,
                playableMetadata);

        UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(expectedEvent.getKind());
        assertThat(event.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

}