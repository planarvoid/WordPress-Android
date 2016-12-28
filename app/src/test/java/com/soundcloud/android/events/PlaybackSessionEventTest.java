package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.mockito.Mock;

import android.support.annotation.NonNull;

import java.util.Arrays;

public class PlaybackSessionEventTest extends AndroidUnitTest {

    private static final long DURATION = 1000L;
    private static final long PROGRESS = 12345L;
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn CREATOR_URN = Urn.forUser(2L);
    private static final String PROTOCOL = "hls";
    private static final TrackItem TRACK_DATA = TestPropertySets.expectedTrackForAnalytics(TRACK_URN,
                                                                                           CREATOR_URN,
                                                                                           "allow",
                                                                                           DURATION);

    private static final TrackItem PROMOTED_TRACK_DATA = TestPropertySets.expectedTrackForPlayer();
    private static final String PLAYER_TYPE = "PLAYA";
    private static final String UUID = "uuid";
    private static final String PLAY_ID = "play-id";

    @Mock TrackSourceInfo trackSourceInfo;

    @Test
    public void stopEventSetsTimeElapsedSinceLastPlayEvent() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs());

        final PlaybackSessionEventArgs args = createArgs();
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(playEvent,
                                                                      PlaybackSessionEvent.STOP_REASON_BUFFERING,
                                                                      args);
        assertThat(stopEvent.getListenTime()).isEqualTo(stopEvent.getTimestamp() - playEvent.getTimestamp());
    }

    @Test
    public void stopEventSetsStopReason() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs());
        final PlaybackSessionEventArgs args = createArgs();
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(playEvent,
                                                                      PlaybackSessionEvent.STOP_REASON_BUFFERING,
                                                                      args);
        assertThat(stopEvent.getStopReason()).isEqualTo(PlaybackSessionEvent.STOP_REASON_BUFFERING);
    }

    @Test
    public void checkpointEventIsCreated() {
        PlaybackSessionEvent checkpointEvent = PlaybackSessionEvent.forCheckpoint(createArgs());
        assertThat(checkpointEvent.isCheckpointEvent()).isTrue();
    }

    @Test
    public void checkpointEventHasTrackProperties() {
        PlaybackSessionEvent checkpointEvent = PlaybackSessionEvent.forCheckpoint(createArgs());
        assertThat(checkpointEvent.getTrackUrn()).isEqualTo(TRACK_URN);
    }

    @Test
    public void noMonetizationTypeIndicatesNotPromotedTrack() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs());
        assertThat(playEvent.isPromotedTrack()).isFalse();
    }

    @Test
    public void eventWithPromotedMonetizationTypeIndicatesAPromotedTrack() {
        PromotedSourceInfo promotedInfo = new PromotedSourceInfo("ad:urn:123",
                                                                 Urn.forTrack(123L),
                                                                 Optional.<Urn>absent(),
                                                                 Arrays.asList("url"));

        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(
                createArgs(0L, PROMOTED_TRACK_DATA)).withPromotedTrack(promotedInfo);

        assertThat(playEvent.isPromotedTrack()).isTrue();
    }

    @Test
    public void eventForPromotedTrackReportsAdStartBasedOnSource() {
        PromotedSourceInfo promotedInfo = new PromotedSourceInfo("ad:urn:123",
                                                                 Urn.forTrack(123L),
                                                                 Optional.<Urn>absent(),
                                                                 Arrays.asList("url"));

        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(
                createArgs(0L, PROMOTED_TRACK_DATA)).withPromotedTrack(promotedInfo);

        assertThat(playEvent.shouldReportAdStart()).isTrue();
    }

    @Test
    public void eventForPromotedTrackDoesNotReportAdStartOnMultiplePlayEventsInSameSession() {
        PromotedSourceInfo promotedInfo = new PromotedSourceInfo("ad:urn:123",
                                                                 Urn.forTrack(123L),
                                                                 Optional.<Urn>absent(),
                                                                 Arrays.asList("url"));
        promotedInfo.setPlaybackStarted();

        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(
                createArgs(0L, PROMOTED_TRACK_DATA)).withPromotedTrack(promotedInfo);

        assertThat(playEvent.shouldReportAdStart()).isFalse();
    }

    @Test
    public void eventWithMarketablePlayIndicatesMarketablePlay() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(PlaybackSessionEventArgs.create(TRACK_DATA,
                                                                                                      trackSourceInfo,
                                                                                                      0L,
                                                                                                      PROTOCOL,
                                                                                                      PLAYER_TYPE,
                                                                                                      false,
                                                                                                      true,
                                                                                                      UUID,
                                                                                                      PLAY_ID));
        assertThat(playEvent.isMarketablePlay()).isTrue();
    }

    @NonNull
    private PlaybackSessionEventArgs createArgs() {
        return createArgs(PROGRESS, TRACK_DATA);
    }

    @NonNull
    private PlaybackSessionEventArgs createArgs(long progress, TrackItem trackData) {
        return PlaybackSessionEventArgs.create(trackData,
                                               trackSourceInfo,
                                               progress,
                                               PROTOCOL,
                                               PLAYER_TYPE,
                                               false,
                                               false,
                                               UUID,
                                               PLAY_ID);
    }

}
