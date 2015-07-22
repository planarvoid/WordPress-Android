package com.soundcloud.android.peripherals;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class PeripheralsControllerTest {

    private PeripheralsController controller;

    private TestEventBus eventBus = new TestEventBus();

    @Mock
    private Context context;

    @Mock
    private TrackRepository trackRepository;

    @Captor
    private ArgumentCaptor<Intent> captor;

    @Before
    public void setUp() {
        controller = new PeripheralsController(context, eventBus, trackRepository);
        controller.subscribe();
    }

    @Test
    public void shouldSendBroadcastWithNotPlayingExtraOnSubscribingToPlaybackStateChangedQueue() {
        verify(context).sendBroadcast(captor.capture());
        Intent firstBroadcast = captor.getAllValues().get(0);
        expect(firstBroadcast.getExtras().get("playing")).toEqual(false);
    }

    @Test
    public void shouldSendBroadcastWithPlayingExtraOnReceivingPlaybackState() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());

        Intent secondBroadcast = verifyTwoBroadcastsSentAndCaptureTheSecond();
        expect(secondBroadcast.getExtras().get("playing")).toEqual(true);
    }

    @Test
    public void shouldSendBroadcastWithPlayingExtraOnReceivingIdlePlayState() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.idle());

        Intent secondBroadcast = verifyTwoBroadcastsSentAndCaptureTheSecond();
        expect(secondBroadcast.getExtras().get("playing")).toEqual(false);
    }

    @Test
    public void shouldSendBroadcastWithPlayStateActionOnReceivingPlaybackStateChange() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());

        Intent secondBroadcast = verifyTwoBroadcastsSentAndCaptureTheSecond();
        expect(secondBroadcast.getAction()).toEqual("com.android.music.playstatechanged");
    }

    @Test
    public void shouldBroadcastTrackInformationWhenThePlayQueueChanges() {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final Urn trackUrn = track.get(TrackProperty.URN);
        when(trackRepository.track(eq(trackUrn))).thenReturn(Observable.just(track));

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn));

        Intent secondBroadcast = verifyTwoBroadcastsSentAndCaptureTheSecond();
        expect(secondBroadcast.getAction()).toEqual("com.android.music.metachanged");
        expect(secondBroadcast.getExtras().get("id")).toEqual(track.get(TrackProperty.URN).getNumericId());
        expect(secondBroadcast.getExtras().get("artist")).toEqual(track.get(PlayableProperty.CREATOR_NAME));
        expect(secondBroadcast.getExtras().get("track")).toEqual(track.get(PlayableProperty.TITLE));
        expect(secondBroadcast.getExtras().get("duration")).toEqual(track.get(PlayableProperty.DURATION));
    }

    @Test
    public void shouldResetTrackInformationOnUserLogout() {
        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forLogout());

        Intent secondBroadcast = verifyTwoBroadcastsSentAndCaptureTheSecond();
        expect(secondBroadcast.getAction()).toEqual("com.android.music.metachanged");
        expect(secondBroadcast.getExtras().get("id")).toEqual("");
        expect(secondBroadcast.getExtras().get("artist")).toEqual("");
        expect(secondBroadcast.getExtras().get("track")).toEqual("");
        expect(secondBroadcast.getExtras().get("duration")).toEqual(0);
    }

    @Test
    public void shouldNotifyWithAnEmptyArtistName() {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        track.put(PlayableProperty.CREATOR_NAME, "");
        final Urn trackUrn = track.get(TrackProperty.URN);
        when(trackRepository.track(eq(trackUrn))).thenReturn(Observable.just(track));

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(trackUrn));

        Intent secondBroadcast = verifyTwoBroadcastsSentAndCaptureTheSecond();
        expect(secondBroadcast.getExtras().get("artist")).toEqual("");
    }

    private Intent verifyTwoBroadcastsSentAndCaptureTheSecond() {
        verify(context, times(2)).sendBroadcast(captor.capture());
        return captor.getAllValues().get(1);
    }

}
