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
    public void onToggleFooterPlayEmitsPauseEventWhenWasPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        listener.onFooterTogglePlay();

        PlayControlEvent event = eventBus.lastEventOn(EventQueue.PLAY_CONTROL);
        expect(event).toEqual(PlayControlEvent.pause(PlayControlEvent.SOURCE_FOOTER_PLAYER));
    }

    @Test
    public void onToggleFooterPlayEmitsPlayEventWhenWasPaused() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        listener.onFooterTogglePlay();

        PlayControlEvent event = eventBus.lastEventOn(EventQueue.PLAY_CONTROL);
        expect(event).toEqual(PlayControlEvent.play(PlayControlEvent.SOURCE_FOOTER_PLAYER));
    }

    @Test
    public void onToggleFooterPlayTogglesPlaybackViaPlaybackOperations() {
        listener.onFooterTogglePlay();
        verify(playbackOperations).togglePlayback();
    }

    @Test
    public void onTogglePlayEmitsPauseEventWhenWasPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        listener.onTogglePlay();

        PlayControlEvent event = eventBus.lastEventOn(EventQueue.PLAY_CONTROL);
        expect(event).toEqual(PlayControlEvent.pause(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    @Test
    public void onTogglePlayEmitsPlayEventWhenWasPaused() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        listener.onTogglePlay();

        PlayControlEvent event = eventBus.lastEventOn(EventQueue.PLAY_CONTROL);
        expect(event).toEqual(PlayControlEvent.play(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    @Test
    public void onTogglePlayTogglesPlaybackViaPlaybackOperations() {
        listener.onTogglePlay();
        verify(playbackOperations).togglePlayback();
    }

    @Test
    public void onToggleLikeTogglesLikeViaAssociationOperations() {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(Urn.forTrack(123L));
        when(soundAssociationOperations.toggleLike(any(TrackUrn.class), anyBoolean())).thenReturn(Observable.<PropertySet>empty());

        listener.onToggleLike(true);

        verify(soundAssociationOperations).toggleLike(Urn.forTrack(123L), true);
    }

    @Test
    public void onFooterTapEmitsUIEventOpenPlayer() {
        listener.onFooterTap();

        UIEvent event = eventBus.lastEventOn(EventQueue.UI);
        UIEvent expectedEvent = UIEvent.fromPlayerOpen(UIEvent.METHOD_TAP_FOOTER);
        expect(event.getKind()).toEqual(expectedEvent.getKind());
        expect(event.getAttributes()).toEqual(expectedEvent.getAttributes());
    }

    @Test
    public void onFooterTapPostsEventToExpandPlayer() {
        listener.onFooterTap();

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        expect(event.isExpand()).toBeTrue();
    }

    @Test
    public void onPlayerClosePostsEventToClosePlayer() {
        listener.onPlayerClose();

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        expect(event.isCollapse()).toBeTrue();
    }

    @Test
    public void onPlayerCloseEmitsUIEventClosePlayer() {
        listener.onPlayerClose();

        UIEvent event = eventBus.lastEventOn(EventQueue.UI);
        UIEvent expectedEvent = UIEvent.fromPlayerOpen(UIEvent.METHOD_HIDE_BUTTON);
        expect(event.getKind()).toEqual(expectedEvent.getKind());
        expect(event.getAttributes()).toEqual(expectedEvent.getAttributes());
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
        UIEvent expectedEvent = UIEvent.fromPlayerClose(UIEvent.METHOD_CONTENT_INTERACTION);
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
}