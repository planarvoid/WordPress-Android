package com.soundcloud.android.analytics;

import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedFollowingForFollowingsScreen;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedPromotedTrack;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForPlayer;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForWidget;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.EntityMetadata;
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

public class EngagementsTrackingTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123l);
    private static final Urn USER_URN = Urn.forUser(33l);
    private static final PropertySet PROMOTED_TRACK = expectedPromotedTrack();
    private static final PropertySet PLAYER_TRACK = expectedTrackForPlayer();
    private static final PropertySet WIDGET_TRACK = expectedTrackForWidget();
    private static final PropertySet FOLLOWED_USER = expectedFollowingForFollowingsScreen(0);

    private final TestEventBus eventBus = new TestEventBus();

    private EngagementsTracking engagementsTracking;
    @Mock private TrackRepository trackRepository;
    @Mock private UserRepository userRepository;

    @Before
    public void setUp() {
        engagementsTracking = new EngagementsTracking(eventBus, trackRepository, userRepository);
    }

    @Test
    public void testLikeTrackUrnForPromotedTrack() {
        final PromotedTrackItem trackItem = PromotedTrackItem.from(PROMOTED_TRACK);
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(trackItem);
        final EntityMetadata entityMetadata = EntityMetadata.from(PROMOTED_TRACK);
        final EventContextMetadata eventContextMetadata = getTrackContextMetadata();

        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(PROMOTED_TRACK));

        engagementsTracking.likeTrackUrn(TRACK_URN, true, eventContextMetadata, promotedSourceInfo);

        UIEvent expectedEvent = UIEvent.fromToggleLike(true,
                TRACK_URN,
                eventContextMetadata,
                promotedSourceInfo,
                entityMetadata);

        assertExpectedEvent(expectedEvent);
    }

    @Test
    public void testLikeTrackUrnForPlayerTrack() {
        final EntityMetadata entityMetadata = EntityMetadata.from(PLAYER_TRACK);
        final EventContextMetadata eventContextMetadata = getTrackContextMetadata();

        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(PLAYER_TRACK));

        engagementsTracking.likeTrackUrn(TRACK_URN, true, eventContextMetadata, null);

        UIEvent expectedEvent = UIEvent.fromToggleLike(true,
                TRACK_URN,
                eventContextMetadata,
                null,
                entityMetadata);

        assertExpectedEvent(expectedEvent);
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

        assertExpectedEvent(expectedEvent);
    }

    @Test
    public void testFollowUserUrn() {
        final EntityMetadata metadata = EntityMetadata.fromUser(FOLLOWED_USER);

        when(userRepository.userInfo(USER_URN)).thenReturn(Observable.just(FOLLOWED_USER));

        engagementsTracking.followUserUrn(USER_URN, true);

        assertExpectedEvent(UIEvent.fromToggleFollow(true, metadata));
    }

    private void assertExpectedEvent(UIEvent expectedEvent) {
        UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(expectedEvent.getKind());
        assertThat(event.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

    private EventContextMetadata getTrackContextMetadata() {
        return EventContextMetadata.builder()
                .invokerScreen("invoker")
                .contextScreen("context_screen")
                .pageName("page").build();
    }

}
