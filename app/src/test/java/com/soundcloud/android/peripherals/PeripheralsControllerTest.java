package com.soundcloud.android.peripherals;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class PeripheralsControllerTest {

    private PeripheralsController controller;

    @Mock
    private EventBus eventBus;

    @Mock
    private Context context;

    private EventMonitor eventMonitor;

    @Before
    public void setUp() throws Exception {
        controller = new PeripheralsController(context, eventBus);
        eventMonitor = EventMonitor.on(eventBus);
    }

    @Test
    public void shouldNotifyTrackChange() throws Exception {
        Track track = new Track(1L);
        track.setTitle("track title");

        controller.notifyMetaChanged(context, track, false);
        verify(context).sendBroadcast(any(Intent.class));
    }

    @Test
    public void shouldNotifyWithCorrectIntent() throws Exception {
        Track track = new Track(1L);
        User user = new User("soundcloud:users:1");
        user.username = "the artist";
        track.setUser(user);
        track.title = "a title";
        track.duration = 123;

        controller.notifyMetaChanged(context, track, true);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).sendBroadcast(captor.capture());
        expect(captor.getValue().getExtras().get("id")).toEqual(1L);
        expect(captor.getValue().getExtras().get("artist")).toEqual("the artist");
        expect(captor.getValue().getExtras().get("track")).toEqual("a title");
        expect(captor.getValue().getExtras().get("playing")).toEqual(true);
        expect(captor.getValue().getExtras().get("duration")).toEqual(123);
    }

    @Test
    public void shouldSubscribeToPlayableChangeEvent() throws Exception {
        controller.subscribe();

        eventMonitor.verifySubscribedTo(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void shouldSendBroadcastWithPlayingExtraOnReceivingPlaybackStateChange() throws Exception {
        controller.subscribe();

        eventMonitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE));

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).sendBroadcast(captor.capture());
        expect(captor.getValue().getExtras().get("playing")).toEqual(true);
    }

    @Test
    public void shouldSendBroadcastWithPlayStateActionOnReceivingPlaybackStateChange() throws Exception {
        controller.subscribe();

        eventMonitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE));

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).sendBroadcast(captor.capture());
        expect(captor.getValue().getAction()).toEqual("com.android.music.playstatechanged");
    }
}
