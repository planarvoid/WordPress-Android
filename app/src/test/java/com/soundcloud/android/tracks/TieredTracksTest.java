package com.soundcloud.android.tracks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TieredTracksTest {

    @Mock private TieredTrack tieredTrack;

    @Test
    public void shouldIndicateHighTierPreviewIfTrackIsSnippedAndInHighTier() {
        when(tieredTrack.isSnipped()).thenReturn(true);
        when(tieredTrack.isSubHighTier()).thenReturn(true);

        assertThat(TieredTracks.isHighTierPreview(tieredTrack)).isTrue();
    }

    @Test
    public void shouldNotIndicateHighTierPreviewIfTrackIsNotSnipped() {
        when(tieredTrack.isSubHighTier()).thenReturn(true);

        assertThat(TieredTracks.isHighTierPreview(tieredTrack)).isFalse();
    }

    @Test
    public void shouldNotIndicateHighTierPreviewIfTrackIsNotInHighTierSnipped() {
        when(tieredTrack.isSnipped()).thenReturn(true);

        assertThat(TieredTracks.isHighTierPreview(tieredTrack)).isFalse();
    }

    @Test
    public void shouldIndicateHighTierUnlockedTrackIfNotSnippedAndHighTier() {
        when(tieredTrack.isSnipped()).thenReturn(false);
        when(tieredTrack.isSubHighTier()).thenReturn(true);

        assertThat(TieredTracks.isFullHighTierTrack(tieredTrack)).isTrue();
    }

    @Test
    public void shouldNotIndicateHighTierUnlockedTrackIfSnippedAndHighTier() {
        when(tieredTrack.isSnipped()).thenReturn(true);
        when(tieredTrack.isSubHighTier()).thenReturn(true);

        assertThat(TieredTracks.isFullHighTierTrack(tieredTrack)).isFalse();
    }

    @Test
    public void shouldNotIndicateHighTierUnlockedTrackIfNotSnippedAndNotHighTier() {
        when(tieredTrack.isSnipped()).thenReturn(false);
        when(tieredTrack.isSubHighTier()).thenReturn(false);

        assertThat(TieredTracks.isFullHighTierTrack(tieredTrack)).isFalse();
    }
}
