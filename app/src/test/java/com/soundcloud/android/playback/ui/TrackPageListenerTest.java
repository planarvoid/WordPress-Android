package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.helpers.NavigationTargetMatcher.matchesNavigationTarget;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlayerInteractionsTracker;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v7.app.AppCompatActivity;

public class TrackPageListenerTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn USER_URN = Urn.forUser(42L);

    @Mock private PlaySessionController playSessionController;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private LikeOperations likeOperations;
    @Mock private NavigationExecutor navigationExecutor;
    @Mock private TrackRepository trackRepository;
    @Mock private EngagementsTracking engagementsTracking;
    @Mock private Navigator navigator;
    @Mock private PlayerInteractionsTracker playerInteractionsTracker;
    @Mock private EventTracker eventTracker;

    private TestEventBus eventBus = new TestEventBus();

    private TrackPageListener listener;

    @Before
    public void setUp() throws Exception {
        listener = new TrackPageListener(playSessionController, playQueueManager, eventBus,
                                         likeOperations, navigationExecutor, engagementsTracking, navigator, eventTracker, playerInteractionsTracker);
    }

    @Test
    public void onToggleUnlikedTrackLikesViaLikesOperations() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(TRACK_URN));

        listener.onToggleLike(true, TRACK_URN);

        verify(likeOperations).toggleLikeAndForget(TRACK_URN, true);
    }

    @Test
    public void onToggleLikedTrackLikesViaUnlikesOperations() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(TRACK_URN));

        listener.onToggleLike(false, TRACK_URN);

        verify(likeOperations).toggleLikeAndForget(TRACK_URN, false);
    }

    @Test
    public void onToggleLikeEmitsLikeEvent() {
        when(playQueueManager.getScreenTag()).thenReturn("context_screen");
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(TRACK_URN));

        listener.onToggleLike(true, TRACK_URN);

        EventContextMetadata contextMetadata = EventContextMetadata.builder()
                                                                   .pageName("tracks:main")
                                                                   .pageUrn(TRACK_URN)
                                                                   .build();

        verify(engagementsTracking).likeTrackUrn(TRACK_URN, true, contextMetadata, null);
    }

    @Test
    public void onToggleLikeEmitsUnlikeEvent() {
        when(playQueueManager.getScreenTag()).thenReturn("context_screen");
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(TRACK_URN));

        listener.onToggleLike(false, TRACK_URN);

        EventContextMetadata contextMetadata = EventContextMetadata.builder()
                                                                   .pageName("tracks:main")
                                                                   .pageUrn(TRACK_URN)
                                                                   .build();
        verify(engagementsTracking).likeTrackUrn(TRACK_URN, false, contextMetadata, null);
    }

    @Test
    public void onGotoUserEmitsEventToClosePlayer() {
        listener.onGotoUser(activity(), USER_URN);

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        assertThat(event.isAutomaticCollapse()).isTrue();
    }

    @Test
    public void shouldStartProfileActivityOnGotoUserAfterPlayerUICollapsed() {
        AppCompatActivity activity = activity();
        listener.onGotoUser(activity, USER_URN);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(navigator).navigateTo(eq(activity), argThat(matchesNavigationTarget(NavigationTarget.forProfile(USER_URN))));
    }

    @Test
    public void onUpsellStartsUpsellFlow() {
        listener.onUpsell(context(), Urn.forTrack(123));
        verify(navigationExecutor).openUpgrade(context(), UpsellContext.PREMIUM_CONTENT);
    }

    @Test
    public void onUpsellTracksUpsellClick() {
        final Urn track = Urn.forTrack(123);

        listener.onUpsell(context(), track);

        final UpgradeFunnelEvent event = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(event.kind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_CLICK);
        assertThat(event.pageUrn().get()).isEqualTo(track.toString());
    }

    @Test
    public void onPlayQueueOpenTracksUIEvent() {
        listener.onPlayQueue();

        final UIEvent trackingEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_OPEN);
    }

    @Test
    public void onArtistNameClickSendsUIEvent() throws Exception {
        listener.onGotoUser(activity(), USER_URN);

        verify(eventTracker).trackClick(UIEvent.fromItemNavigation(USER_URN, EventContextMetadata.builder().pageName(Screen.PLAYER_MAIN.get()).build()));
    }
}
