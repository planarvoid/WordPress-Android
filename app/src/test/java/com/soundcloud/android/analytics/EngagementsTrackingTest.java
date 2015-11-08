package com.soundcloud.android.analytics;

import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedFollowingForFollowingsScreen;
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
        final PlayableMetadata playableMetadata = PlayableMetadata.from(PROMOTED_TRACK);

        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(PROMOTED_TRACK));

        engagementsTracking.likeTrackUrn(TRACK_URN, true, "invoker", "context_screen", "page", TRACK_URN, promotedSourceInfo);

        UIEvent expectedEvent = UIEvent.fromToggleLike(true,
                "invoker",
                "context_screen",
                "page",
                TRACK_URN,
                TRACK_URN,
                promotedSourceInfo,
                playableMetadata);

        assertExpectedEvent(expectedEvent);
    }

    @Test
    public void testLikeTrackUrnForPlayerTrack() {
        final PlayableMetadata playableMetadata = PlayableMetadata.from(PLAYER_TRACK);

        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(PLAYER_TRACK));

        engagementsTracking.likeTrackUrn(TRACK_URN, true, "invoker", "context_screen", "page", TRACK_URN, null);

        UIEvent expectedEvent = UIEvent.fromToggleLike(true,
                "invoker",
                "context_screen",
                "page",
                TRACK_URN,
                TRACK_URN,
                null,
                playableMetadata);

        assertExpectedEvent(expectedEvent);
    }

    @Test
    public void testLikeTrackUrnForWidgetTrack() {
        final PlayableMetadata playableMetadata = PlayableMetadata.from(WIDGET_TRACK);
        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(WIDGET_TRACK));

        engagementsTracking.likeTrackUrn(TRACK_URN, true, "widget", "context_screen", "widget", Urn.NOT_SET, null);

        UIEvent expectedEvent = UIEvent.fromToggleLike(true,
                "widget",
                "context_screen",
                "widget",
                TRACK_URN,
                Urn.NOT_SET,
                null,
                playableMetadata);

        assertExpectedEvent(expectedEvent);
    }

    @Test
    public void testFollowUserUrn() {
        final PlayableMetadata metadata = PlayableMetadata.fromUser(FOLLOWED_USER);

        when(userRepository.userInfo(USER_URN)).thenReturn(Observable.just(FOLLOWED_USER));

        engagementsTracking.followUserUrn(USER_URN, true);

        assertExpectedEvent(UIEvent.fromToggleFollow(true, metadata));
    }

    private void assertExpectedEvent(UIEvent expectedEvent) {
        UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(expectedEvent.getKind());
        assertThat(event.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

}
