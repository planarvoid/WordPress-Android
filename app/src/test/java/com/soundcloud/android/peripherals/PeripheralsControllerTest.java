package com.soundcloud.android.peripherals;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.strings.Strings;
import io.reactivex.Maybe;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;

public class PeripheralsControllerTest extends AndroidUnitTest {

    @SuppressWarnings("FieldCanBeLocal")
    private PeripheralsController controller;

    @Mock private Context context;
    @Mock private TrackRepository trackRepository;
    @Captor private ArgumentCaptor<Intent> captor;

    @Before
    public void setUp() {
        controller = new PeripheralsController(context, trackRepository);
    }

    @Test
    public void shouldSendBroadcastWithPlayingExtraOnReceivingPlaybackState() {
        controller.onPlayStateEvent(TestPlayStates.playing());

        Intent broadcast = verifyBroadcastSentAndCapture();
        assertThat(broadcast.getExtras().get("playing")).isEqualTo(true);
    }

    @Test
    public void shouldSendBroadcastWithPlayingExtraOnReceivingIdlePlayState() {
        controller.onPlayStateEvent(TestPlayStates.idle());

        Intent broadcast = verifyBroadcastSentAndCapture();
        assertThat(broadcast.getExtras().get("playing")).isEqualTo(false);
    }

    @Test
    public void shouldSendBroadcastWithPlayStateActionOnReceivingPlaybackStateChange() {
        controller.onPlayStateEvent(TestPlayStates.playing());

        Intent broadcast = verifyBroadcastSentAndCapture();
        assertThat(broadcast.getAction()).isEqualTo("com.android.music.playstatechanged");
    }

    @Test
    public void shouldBroadcastTrackInformationWhenThePlayQueueChanges() {
        final Track track = ModelFixtures.trackBuilder().build();
        final Urn trackUrn = track.urn();
        when(trackRepository.track(eq(trackUrn))).thenReturn(Maybe.just(track));

        controller.onCurrentPlayQueueItem(CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(trackUrn),
                                                                                 Urn.NOT_SET,
                                                                                 0));

        Intent broadcast = verifyBroadcastSentAndCapture();
        assertThat(broadcast.getAction()).isEqualTo("com.android.music.metachanged");
        assertThat(broadcast.getExtras().get("id")).isEqualTo(track.urn().getNumericId());
        assertThat(broadcast.getExtras().get("artist")).isEqualTo(track.creatorName());
        assertThat(broadcast.getExtras().get("track")).isEqualTo(track.title());
        assertThat(broadcast.getExtras().get("duration")).isEqualTo(track.fullDuration());
    }

    @Test
    public void shouldResetTrackInformationOnUserLogout() {
        controller.onCurrentUserChanged(CurrentUserChangedEvent.forLogout());

        Intent broadcast = verifyBroadcastSentAndCapture();
        assertThat(broadcast.getAction()).isEqualTo("com.android.music.metachanged");
        assertThat(broadcast.getExtras().get("id")).isEqualTo("");
        assertThat(broadcast.getExtras().get("artist")).isEqualTo("");
        assertThat(broadcast.getExtras().get("track")).isEqualTo("");
        assertThat(broadcast.getExtras().get("duration")).isEqualTo(0);
    }

    @Test
    public void shouldNotifyWithAnEmptyArtistName() {
        final Track track = ModelFixtures.trackBuilder().creatorName(Strings.EMPTY).build();
        final Urn trackUrn = track.urn();
        when(trackRepository.track(eq(trackUrn))).thenReturn(Maybe.just(track));

        controller.onCurrentPlayQueueItem(CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(trackUrn),
                                                                                 Urn.NOT_SET,
                                                                                 0));

        Intent broadcast = verifyBroadcastSentAndCapture();
        assertThat(broadcast.getExtras().get("artist")).isEqualTo("");
    }

    private Intent verifyBroadcastSentAndCapture() {
        verify(context, times(1)).sendBroadcast(captor.capture());
        return captor.getAllValues().get(0);
    }

}
