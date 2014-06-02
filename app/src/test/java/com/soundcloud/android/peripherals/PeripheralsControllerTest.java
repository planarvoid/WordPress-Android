package com.soundcloud.android.peripherals;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.service.Playa.StateTransition;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.track.TrackOperations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;

import android.content.Context;
import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class PeripheralsControllerTest {

    private PeripheralsController controller;

    @Mock
    private Context context;

    @Mock
    private PlayQueueManager playQueueManager;

    @Mock
    private TrackOperations trackOperations;

    @Mock
    private EventBus eventBus;

    private EventMonitor eventMonitor;

    @Captor
    private ArgumentCaptor<Intent> captor;

    @Before
    public void setUp() throws Exception {
        controller = new PeripheralsController(context, eventBus, playQueueManager, trackOperations);
        eventMonitor = EventMonitor.on(eventBus);
        controller.subscribe();
    }

    @Test
    public void shouldSubscribeToPlayableChangeEvents() throws Exception {
        eventMonitor.verifySubscribedTo(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void shouldSendBroadcastWithPlayingExtraOnReceivingPlaybackState() throws Exception {
        eventMonitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE));

        verifyBroadcastSentAndCapture();
        expect(captor.getValue().getExtras().get("playing")).toEqual(true);
    }

    @Test
    public void shouldSendBroadcastWithPlayingExtraOnReceivingIdlePlayState() throws Exception {
        eventMonitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE));

        verifyBroadcastSentAndCapture();
        expect(captor.getValue().getExtras().get("playing")).toEqual(false);
    }

    @Test
    public void shouldSendBroadcastWithPlayStateActionOnReceivingPlaybackStateChange() throws Exception {
        eventMonitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE));

        verifyBroadcastSentAndCapture();
        expect(captor.getValue().getAction()).toEqual("com.android.music.playstatechanged");
    }

    @Test
    public void shouldSubscribeToThePlayQueueEvents() throws Exception {
        eventMonitor.verifySubscribedTo(EventQueue.PLAY_QUEUE);
    }

    @Test
    public void shouldBroadcastTrackInformationWhenThePlayQueueChanges() throws Exception {
        final long currentTrackId = 3L;
        when(playQueueManager.getCurrentTrackId()).thenReturn(currentTrackId);
        final Track track = createTrack();
        when(trackOperations.loadTrack(eq(currentTrackId), any(Scheduler.class))).thenReturn(Observable.just(track));

        eventMonitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(track.getUrn()));

        verifyBroadcastSentAndCapture();
        expect(captor.getValue().getAction()).toEqual("com.android.music.metachanged");
        expect(captor.getValue().getExtras().get("id")).toEqual(track.getId());
        expect(captor.getValue().getExtras().get("artist")).toEqual(track.user.username);
        expect(captor.getValue().getExtras().get("track")).toEqual(track.title);
        expect(captor.getValue().getExtras().get("duration")).toEqual(track.duration);
    }

    @Test
    public void shouldSubscribeToCurrentUserChangedEvent() throws Exception {
        eventMonitor.verifySubscribedTo(EventQueue.CURRENT_USER_CHANGED);
    }

    @Test
    public void shouldResetTrackInformationOnUserLogout() throws Exception {
        eventMonitor.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forLogout());

        verifyBroadcastSentAndCapture();
        expect(captor.getValue().getAction()).toEqual("com.android.music.metachanged");
        expect(captor.getValue().getExtras().get("id")).toEqual("");
        expect(captor.getValue().getExtras().get("artist")).toEqual("");
        expect(captor.getValue().getExtras().get("track")).toEqual("");
        expect(captor.getValue().getExtras().get("duration")).toEqual(0);
    }

    private void verifyBroadcastSentAndCapture() {
        verify(context).sendBroadcast(captor.capture());
    }

    private Track createTrack() {
        Track track = new Track(1L);
        User user = new User("soundcloud:users:1");
        user.username = "the artist";
        track.setUser(user);
        track.title = "a title";
        track.duration = 123;
        return track;
    }
}
