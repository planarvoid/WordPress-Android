package com.soundcloud.android.peripherals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.content.Intent;

public class PeripheralsControllerTest extends AndroidUnitTest {

    @SuppressWarnings("FieldCanBeLocal")
    private PeripheralsController controller;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private Context context;
    @Mock private TrackRepository trackRepository;
    @Captor private ArgumentCaptor<Intent> captor;

    @Before
    public void setUp() {
        controller = new PeripheralsController(context, eventBus, trackRepository);
        controller.subscribe();
    }

    @Test
    public void shouldSendBroadcastWithNotPlayingExtraOnSubscribingToPlaybackStateChangedQueue() {
        verify(context).sendBroadcast(captor.capture());
        Intent firstBroadcast = captor.getAllValues().get(0);
        assertThat(firstBroadcast.getExtras().get("playing")).isEqualTo(false);
    }

    @Test
    public void shouldSendBroadcastWithPlayingExtraOnReceivingPlaybackState() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());

        Intent secondBroadcast = verifyTwoBroadcastsSentAndCaptureTheSecond();
        assertThat(secondBroadcast.getExtras().get("playing")).isEqualTo(true);
    }

    @Test
    public void shouldSendBroadcastWithPlayingExtraOnReceivingIdlePlayState() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.idle());

        Intent secondBroadcast = verifyTwoBroadcastsSentAndCaptureTheSecond();
        assertThat(secondBroadcast.getExtras().get("playing")).isEqualTo(false);
    }

    @Test
    public void shouldSendBroadcastWithPlayStateActionOnReceivingPlaybackStateChange() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());

        Intent secondBroadcast = verifyTwoBroadcastsSentAndCaptureTheSecond();
        assertThat(secondBroadcast.getAction()).isEqualTo("com.android.music.playstatechanged");
    }

    @Test
    public void shouldBroadcastTrackInformationWhenThePlayQueueChanges() {
        final Track track = ModelFixtures.trackBuilder().build();
        final Urn trackUrn = track.urn();
        when(trackRepository.track(eq(trackUrn))).thenReturn(Observable.just(track));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(trackUrn),
                                                                Urn.NOT_SET,
                                                                0));

        Intent secondBroadcast = verifyTwoBroadcastsSentAndCaptureTheSecond();
        assertThat(secondBroadcast.getAction()).isEqualTo("com.android.music.metachanged");
        assertThat(secondBroadcast.getExtras().get("id")).isEqualTo(track.urn().getNumericId());
        assertThat(secondBroadcast.getExtras().get("artist")).isEqualTo(track.creatorName().get());
        assertThat(secondBroadcast.getExtras().get("track")).isEqualTo(track.title());
        assertThat(secondBroadcast.getExtras().get("duration")).isEqualTo(track.fullDuration());
    }

    @Test
    public void shouldResetTrackInformationOnUserLogout() {
        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forLogout());

        Intent secondBroadcast = verifyTwoBroadcastsSentAndCaptureTheSecond();
        assertThat(secondBroadcast.getAction()).isEqualTo("com.android.music.metachanged");
        assertThat(secondBroadcast.getExtras().get("id")).isEqualTo("");
        assertThat(secondBroadcast.getExtras().get("artist")).isEqualTo("");
        assertThat(secondBroadcast.getExtras().get("track")).isEqualTo("");
        assertThat(secondBroadcast.getExtras().get("duration")).isEqualTo(0);
    }

    @Test
    public void shouldNotifyWithAnEmptyArtistName() {
        final Track track = ModelFixtures.trackBuilder().creatorName(Optional.absent()).build();
        final Urn trackUrn = track.urn();
        when(trackRepository.track(eq(trackUrn))).thenReturn(Observable.just(track));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(trackUrn),
                                                                Urn.NOT_SET,
                                                                0));

        Intent secondBroadcast = verifyTwoBroadcastsSentAndCaptureTheSecond();
        assertThat(secondBroadcast.getExtras().get("artist")).isEqualTo("");
    }

    private Intent verifyTwoBroadcastsSentAndCaptureTheSecond() {
        verify(context, times(2)).sendBroadcast(captor.capture());
        return captor.getAllValues().get(1);
    }

}
