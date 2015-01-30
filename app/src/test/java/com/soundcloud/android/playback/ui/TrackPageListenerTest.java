package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class TrackPageListenerTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);

    @Mock private PlaybackOperations playbackOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private FeatureFlags featureFlags;
    @Mock private LikeOperations likeOperations;

    private TestEventBus eventBus = new TestEventBus();

    private TrackPageListener listener;

    @Before
    public void setUp() throws Exception {
        listener = new TrackPageListener(playbackOperations,
                playQueueManager,
                playSessionStateProvider, eventBus, likeOperations);
    }

    @Test
    public void onToggleUnlikedTrackLikesViaLikesOperations() {
        when(likeOperations.addLike(any(PropertySet.class))).thenReturn(Observable.<PropertySet>empty());

        listener.onToggleLike(true, TRACK_URN);

        verify(likeOperations).addLike(PropertySet.from(PlayableProperty.URN.bind(TRACK_URN)));
    }

    @Test
    public void onToggleLikedTrackLikesViaUnlikesOperations() {
        when(likeOperations.removeLike(any(PropertySet.class))).thenReturn(Observable.<PropertySet>empty());

        listener.onToggleLike(false, TRACK_URN);

        verify(likeOperations).removeLike(PropertySet.from(PlayableProperty.URN.bind(TRACK_URN)));
    }

    @Test
    public void onToggleLikeEmitsLikeEvent() {
        when(playQueueManager.getScreenTag()).thenReturn("context_screen");
        when(likeOperations.addLike(any(PropertySet.class))).thenReturn(Observable.<PropertySet>empty());

        listener.onToggleLike(true, TRACK_URN);

        UIEvent expectedEvent = UIEvent.fromToggleLike(true, "player", "context_screen", TRACK_URN);
        expectUIEvent(expectedEvent);
    }

    @Test
    public void onToggleLikeEmitsUnlikeEvent() {
        when(playQueueManager.getScreenTag()).thenReturn("context_screen");
        when(likeOperations.removeLike(any(PropertySet.class))).thenReturn(Observable.<PropertySet>empty());

        listener.onToggleLike(false, TRACK_URN);

        UIEvent expectedEvent = UIEvent.fromToggleLike(false, "player", "context_screen", TRACK_URN);
        expectUIEvent(expectedEvent);
    }

    @Test
    public void onGotoUserEmitsEventToClosePlayer() {
        Urn userUrn = Urn.forUser(42L);

        listener.onGotoUser(Robolectric.application, userUrn);

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        expect(event.isCollapse()).toBeTrue();
    }

    @Test
    public void onGotoUserEmitsUIEventClosePlayer() {
        listener.onGotoUser(Robolectric.application, Urn.forUser(42L));

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        UIEvent expectedEvent = UIEvent.fromPlayerClose(UIEvent.METHOD_PROFILE_OPEN);
        expect(event.getKind()).toEqual(expectedEvent.getKind());
        expect(event.getAttributes()).toEqual(expectedEvent.getAttributes());
    }

    @Test
    public void onScrubbingShouldEmitPlayerControlScrubEvent() {
        listener.onScrub(ScrubController.SCRUB_STATE_SCRUBBING);

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event).toEqual(PlayControlEvent.scrub(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    @Test
    public void onScrubbingCancelledShouldNotEmitPlayerControlScrubEvent() {
        listener.onScrub(ScrubController.SCRUB_STATE_CANCELLED);
        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void shouldStartProfileActivityOnGotoUserAfterPlayerUICollapsed() {
        Urn userUrn = Urn.forUser(42L);

        listener.onGotoUser(Robolectric.application, userUrn);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        final Intent nextStartedActivity = Robolectric.shadowOf(Robolectric.application).getNextStartedActivity();
        expect(nextStartedActivity).not.toBeNull();
        expect(nextStartedActivity.getComponent().getClassName()).toEqual(ProfileActivity.class.getCanonicalName());
        expect(nextStartedActivity.getExtras().get("userUrn")).toEqual(userUrn);
    }

    private void expectUIEvent(UIEvent expectedEvent) {
        TrackingEvent uiEvent = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toEqual(expectedEvent.getKind());
        expect(uiEvent.getAttributes()).toEqual(expectedEvent.getAttributes());
    }
}