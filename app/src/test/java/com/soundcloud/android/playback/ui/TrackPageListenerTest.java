package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.users.UserUrn;
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

    private static final TrackUrn TRACK_URN = Urn.forTrack(123L);

    @Mock private PlaybackOperations playbackOperations;
    @Mock private SoundAssociationOperations soundAssociationOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;

    private TestEventBus eventBus = new TestEventBus();

    private TrackPageListener listener;

    @Before
    public void setUp() throws Exception {
        listener = new TrackPageListener(playbackOperations,
                soundAssociationOperations, playQueueManager,
                playSessionStateProvider, eventBus);
    }

    @Test
    public void onToggleLikeTogglesLikeViaAssociationOperations() {
        when(soundAssociationOperations.toggleLike(any(TrackUrn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());

        listener.onToggleLike(true, TRACK_URN);

        verify(soundAssociationOperations).toggleLike(TRACK_URN, true);
    }

    @Test
    public void onToggleLikeEmitsLikeEvent() {
        when(playQueueManager.getScreenTag()).thenReturn("screen");
        when(soundAssociationOperations.toggleLike(any(TrackUrn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());

        listener.onToggleLike(true, TRACK_URN);

        UIEvent expectedEvent = UIEvent.fromToggleLike(true, "screen", TRACK_URN);
        expectUIEvent(expectedEvent);
    }

    @Test
    public void onToggleLikeEmitsUnlikeEvent() {
        when(playQueueManager.getScreenTag()).thenReturn("screen");
        when(soundAssociationOperations.toggleLike(any(TrackUrn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());

        listener.onToggleLike(false, TRACK_URN);

        UIEvent expectedEvent = UIEvent.fromToggleLike(false, "screen", TRACK_URN);
        expectUIEvent(expectedEvent);
    }

    @Test
    public void onGotoUserEmitsEventToClosePlayer() {
        UserUrn userUrn = Urn.forUser(42L);

        listener.onGotoUser(Robolectric.application, userUrn);

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        expect(event.isCollapse()).toBeTrue();
    }

    @Test
    public void onGotoUserEmitsUIEventClosePlayer() {
        listener.onGotoUser(Robolectric.application, Urn.forUser(42L));

        UIEvent event = eventBus.lastEventOn(EventQueue.UI);
        UIEvent expectedEvent = UIEvent.fromPlayerClose(UIEvent.METHOD_PROFILE_OPEN);
        expect(event.getKind()).toEqual(expectedEvent.getKind());
        expect(event.getAttributes()).toEqual(expectedEvent.getAttributes());
    }

    @Test
    public void onScrubbingShouldEmitPlayerControlScrubEvent() {
        listener.onScrub(ScrubController.SCRUB_STATE_SCRUBBING);

        PlayControlEvent event = eventBus.lastEventOn(EventQueue.PLAY_CONTROL);
        expect(event).toEqual(PlayControlEvent.scrub(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    @Test
    public void onScrubbingCancelledShouldNotEmitPlayerControlScrubEvent() {
        listener.onScrub(ScrubController.SCRUB_STATE_CANCELLED);
        eventBus.verifyNoEventsOn(EventQueue.PLAY_CONTROL);
    }

    @Test
    public void shouldStartProfileActivityOnGotoUserAfterPlayerUICollapsed() {
        UserUrn userUrn = Urn.forUser(42L);

        listener.onGotoUser(Robolectric.application, userUrn);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        final Intent nextStartedActivity = Robolectric.shadowOf(Robolectric.application).getNextStartedActivity();
        expect(nextStartedActivity).not.toBeNull();
        expect(nextStartedActivity.getComponent().getClassName()).toEqual(ProfileActivity.class.getCanonicalName());
        expect(nextStartedActivity.getExtras().get("userUrn")).toEqual(UserUrn.forUser(42L));
    }

    private void expectUIEvent(UIEvent expectedEvent) {
        UIEvent uiEvent = eventBus.lastEventOn(EventQueue.UI);
        expect(uiEvent.getKind()).toEqual(expectedEvent.getKind());
        expect(uiEvent.getAttributes()).toEqual(expectedEvent.getAttributes());
    }
}