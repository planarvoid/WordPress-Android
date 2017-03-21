package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

public class PlaybackProgressRepositoryTest extends AndroidUnitTest {
    private static final long POSITION = 23456L;
    private static final long DURATION = 456789L;
    private static final Urn URN = Urn.forTrack(127L);

    @Mock TrackRepository trackRepo;

    private PlaybackProgressRepository playbackProgressRepository;

    @Before
    public void setUp() throws Exception {
        this.playbackProgressRepository = new PlaybackProgressRepository(trackRepo);
    }

    @Test
    public void cachedProgressValueBasedOnUrn() throws Exception {
        PlaybackProgress progress = playbackProgress();
        playbackProgressRepository.put(URN, progress);

        assertThat(playbackProgressRepository.get(URN).get()).isEqualTo(progress);
    }

    @Test
    public void putForPositionWillUpdateCachedPositionByMergingItWithExistingDurationWithNoDatabaseLookup() {
        playbackProgressRepository.put(URN, playbackProgress()); // pre-populate cache
        long newPosition = POSITION + 200L;

        playbackProgressRepository.put(URN, newPosition);

        verifyZeroInteractions(trackRepo);
        PlaybackProgress cached = playbackProgressRepository.get(URN).get();
        assertThat(cached.getPosition()).isEqualTo(newPosition);
    }

    @Test
    public void putForPositionOnlyWillGetDurationFromDatabaseToCacheIt() throws Exception {
        final Track track = ModelFixtures.trackBuilder().build();
        when(trackRepo.track(URN)).thenReturn(Observable.just(track));

        playbackProgressRepository.put(URN, POSITION);

        Optional<PlaybackProgress> cachedPlaybackProgress = playbackProgressRepository.get(URN);
        assertThat(cachedPlaybackProgress.isPresent()).isTrue();
        assertThat(cachedPlaybackProgress.get().getPosition()).isEqualTo(POSITION);
        assertThat(cachedPlaybackProgress.get().getDuration()).isEqualTo(track.fullDuration());
        assertThat(cachedPlaybackProgress.get().getUrn()).isEqualTo(URN);
    }

    @Test
    public void removeClearsCacheOfProgressForGivenUrn() throws Exception {
        playbackProgressRepository.put(URN, playbackProgress());

        playbackProgressRepository.remove(URN);

        assertThat(playbackProgressRepository.get(URN).isPresent()).isFalse();
    }

    @Test
    public void getReturnsAbsentOptionalIfThereIsNoCachedValue() throws Exception {
        assertThat(playbackProgressRepository.get(URN).isPresent()).isFalse();
    }

    private PlaybackProgress playbackProgress() {
        return new PlaybackProgress(POSITION, DURATION, URN);
    }
}
