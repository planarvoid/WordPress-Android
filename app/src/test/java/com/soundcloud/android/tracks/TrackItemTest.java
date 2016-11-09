package com.soundcloud.android.tracks;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;

public class TrackItemTest extends AndroidUnitTest {
    private static final Urn CURRENT_TRACK_URN = Urn.forTrack(124L);
    private TrackItem trackItem;

    @Before
    public void setUp() throws Exception {
        trackItem = ModelFixtures.create(TrackItem.class);
    }

    @Test
    public void setsNowPlayingWhenUrnMatchesAndNotCurrentlyPlaying() throws Exception {
        trackItem.setIsPlaying(false);

        final boolean updated = trackItem.updateNowPlaying(trackItem.getUrn());

        assertThat(trackItem.isPlaying()).isTrue();
        assertThat(updated).isTrue();
    }

    @Test
    public void removesNowPlayingWhenUrnDoesNotMatchAndPlaying() throws Exception {
        trackItem.setIsPlaying(true);

        final boolean updated = trackItem.updateNowPlaying(CURRENT_TRACK_URN);

        assertThat(trackItem.isPlaying()).isFalse();
        assertThat(updated).isTrue();
    }

    @Test
    public void doesNotSetNowPlayingWhenDifferentUrnAndNotPlaying() throws Exception {
        trackItem.setIsPlaying(false);

        final boolean updated = trackItem.updateNowPlaying(CURRENT_TRACK_URN);

        assertThat(trackItem.isPlaying()).isFalse();
        assertThat(updated).isFalse();
    }
}
