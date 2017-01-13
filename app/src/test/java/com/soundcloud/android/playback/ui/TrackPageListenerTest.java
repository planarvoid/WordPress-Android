package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableTrackingKeys;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;

public class TrackPageListenerTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);

    @Mock private PlaySessionController playSessionController;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private LikeOperations likeOperations;
    @Mock private Navigator navigator;
    @Mock private TrackRepository trackRepository;
    @Mock private EngagementsTracking engagementsTracking;

    private TestEventBus eventBus = new TestEventBus();

    private TrackPageListener listener;

    @Before
    public void setUp() throws Exception {
        listener = new TrackPageListener(playSessionController, playQueueManager, eventBus,
                                         likeOperations, navigator, engagementsTracking);
    }

    @Test
    public void onToggleUnlikedTrackLikesViaLikesOperations() {
        when(likeOperations.toggleLike(TRACK_URN, true)).thenReturn(Observable.empty());
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(TRACK_URN));

        listener.onToggleLike(true, TRACK_URN);

        verify(likeOperations).toggleLike(TRACK_URN, true);
    }

    @Test
    public void onToggleLikedTrackLikesViaUnlikesOperations() {
        when(likeOperations.toggleLike(TRACK_URN, false)).thenReturn(Observable.empty());
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(TRACK_URN));

        listener.onToggleLike(false, TRACK_URN);

        verify(likeOperations).toggleLike(TRACK_URN, false);
    }

    @Test
    public void onToggleLikeEmitsLikeEvent() {
        when(playQueueManager.getScreenTag()).thenReturn("context_screen");
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(TRACK_URN));
        when(likeOperations.toggleLike(TRACK_URN, true)).thenReturn(Observable.empty());

        listener.onToggleLike(true, TRACK_URN);

        EventContextMetadata contextMetadata = EventContextMetadata.builder()
                                                                   .invokerScreen("player")
                                                                   .contextScreen("context_screen")
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
        when(likeOperations.toggleLike(TRACK_URN, false)).thenReturn(Observable.empty());

        listener.onToggleLike(false, TRACK_URN);

        EventContextMetadata contextMetadata = EventContextMetadata.builder()
                                                                   .invokerScreen("player")
                                                                   .contextScreen("context_screen")
                                                                   .pageName("tracks:main")
                                                                   .pageUrn(TRACK_URN)
                                                                   .build();
        verify(engagementsTracking).likeTrackUrn(TRACK_URN, false, contextMetadata, null);
    }

    @Test
    public void onGotoUserEmitsEventToClosePlayer() {
        Urn userUrn = Urn.forUser(42L);

        listener.onGotoUser(context(), userUrn);

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        assertThat(event.isCollapse()).isTrue();
    }

    @Test
    public void shouldStartProfileActivityOnGotoUserAfterPlayerUICollapsed() {
        Urn userUrn = Urn.forUser(42L);

        listener.onGotoUser(context(), userUrn);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(navigator).legacyOpenProfile(any(Context.class), eq(userUrn));
    }

    @Test
    public void onUpsellStartsUpsellFlow() {
        listener.onUpsell(context(), Urn.forTrack(123));
        verify(navigator).openUpgrade(context());
    }

    @Test
    public void onUpsellTracksUpsellClick() {
        final Urn track = Urn.forTrack(123);

        listener.onUpsell(context(), track);

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(UpgradeFunnelEvent.KIND_UPSELL_CLICK);
        assertThat(event.get(PlayableTrackingKeys.KEY_PAGE_URN)).isEqualTo(track.toString());
    }

    @Test
    public void onPlayQueueOpenTracksUIEvent() {
        listener.onPlayQueue();

        final UIEvent trackingEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_OPEN);
    }
}
