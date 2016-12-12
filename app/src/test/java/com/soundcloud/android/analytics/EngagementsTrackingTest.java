package com.soundcloud.android.analytics;

import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedFollowingForFollowingsScreen;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedPromotedTrack;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForPlayer;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForWidget;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

public class EngagementsTrackingTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn USER_URN = Urn.forUser(33L);
    private static final PropertySet PROMOTED_TRACK = expectedPromotedTrack();
    private static final PropertySet PLAYER_TRACK = expectedTrackForPlayer();
    private static final PropertySet WIDGET_TRACK = expectedTrackForWidget();
    private static final PropertySet FOLLOWED_USER = expectedFollowingForFollowingsScreen(0);

    private final TestEventBus eventBus = new TestEventBus();

    private EngagementsTracking engagementsTracking;
    private TestSubscriber<UIEvent> testSubscriber;

    @Mock private TrackRepository trackRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventTracker eventTracker;

    @Before
    public void setUp() {
        testSubscriber = new TestSubscriber<>();

        when(eventTracker.trackEngagementSubscriber()).thenReturn(testSubscriber);

        engagementsTracking = new EngagementsTracking(trackRepository, userRepository, eventTracker);
    }

    @Test
    public void testLikeTrackUrnForPromotedTrack() {
        final PromotedTrackItem trackItem = PromotedTrackItem.from(PROMOTED_TRACK);
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(trackItem);
        final EntityMetadata entityMetadata = EntityMetadata.from(PROMOTED_TRACK);
        final EventContextMetadata eventContextMetadata = getEventContextMetadata();

        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(PROMOTED_TRACK));

        engagementsTracking.likeTrackUrn(TRACK_URN, true, eventContextMetadata, promotedSourceInfo);

        UIEvent expectedEvent = UIEvent.fromToggleLike(true,
                                                       TRACK_URN,
                                                       eventContextMetadata,
                                                       promotedSourceInfo,
                                                       entityMetadata);

        assertCorrectEvent(testSubscriber.getOnNextEvents().get(0), expectedEvent);
    }

    @Test
    public void testLikeTrackUrnForPlayerTrack() {
        final EntityMetadata entityMetadata = EntityMetadata.from(PLAYER_TRACK);
        final EventContextMetadata eventContextMetadata = getEventContextMetadata();

        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(PLAYER_TRACK));

        engagementsTracking.likeTrackUrn(TRACK_URN, true, eventContextMetadata, null);

        UIEvent expectedEvent = UIEvent.fromToggleLike(true,
                                                       TRACK_URN,
                                                       eventContextMetadata,
                                                       null,
                                                       entityMetadata);

        assertCorrectEvent(testSubscriber.getOnNextEvents().get(0), expectedEvent);
    }

    @Test
    public void testLikeTrackUrnForWidgetTrack() {
        final EntityMetadata entityMetadata = EntityMetadata.from(WIDGET_TRACK);
        final EventContextMetadata eventContextMetadata = EventContextMetadata.builder()
                                                                              .invokerScreen("widget")
                                                                              .contextScreen("context_screen")
                                                                              .pageName("widget").build();
        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(WIDGET_TRACK));
        engagementsTracking.likeTrackUrn(TRACK_URN, true, eventContextMetadata, null);

        UIEvent expectedEvent = UIEvent.fromToggleLike(true,
                                                       TRACK_URN,
                                                       eventContextMetadata,
                                                       null,
                                                       entityMetadata);

        assertCorrectEvent(testSubscriber.getOnNextEvents().get(0), expectedEvent);
    }

    @Test
    public void testFollowUserUrn() {
        final EntityMetadata metadata = EntityMetadata.fromUser(FOLLOWED_USER);
        final EventContextMetadata eventContextMetadata = getEventContextMetadata();

        when(userRepository.userInfo(USER_URN)).thenReturn(Observable.just(FOLLOWED_USER));

        engagementsTracking.followUserUrn(USER_URN, true, eventContextMetadata);

        final UIEvent expectedEvent = UIEvent.fromToggleFollow(true, metadata, eventContextMetadata);

        assertCorrectEvent(testSubscriber.getOnNextEvents().get(0), expectedEvent);
    }

    private void assertCorrectEvent(UIEvent event, UIEvent expectedEvent) {
        assertThat(event.kind()).isEqualTo(expectedEvent.kind());
        if (event.contextScreen().isPresent() && expectedEvent.contextScreen().isPresent()) {
            assertThat(event.contextScreen().get()).isEqualTo(expectedEvent.contextScreen().get());
        }
        if (event.module().isPresent() && expectedEvent.module().isPresent()) {
            assertThat(event.module().get()).isEqualTo(expectedEvent.module().get());
        }
    }

    private EventContextMetadata getEventContextMetadata() {
        return EventContextMetadata.builder()
                                   .invokerScreen("invoker")
                                   .contextScreen("context_screen")
                                   .pageName("page").build();
    }
}
