package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.TrackUrn;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackProgressInfoTest {

    @Test
    public void shouldResumeTrackReturnsFalseForDifferentTrack() throws Exception {
        expect(new PlaybackProgressInfo(1L, 100L).shouldResumeTrack(TrackUrn.forTrack(2L))).toBeFalse();
    }

    @Test
    public void shouldResumeTrackReturnsFalseWithoutProgress() throws Exception {
        expect(new PlaybackProgressInfo(1L, 0L).shouldResumeTrack(TrackUrn.forTrack(1L))).toBeFalse();
    }

    @Test
    public void shouldResumeTrackReturnsTrueWithoutPositiveProgressForSameTrack() throws Exception {
        expect(new PlaybackProgressInfo(1L, 10L).shouldResumeTrack(TrackUrn.forTrack(1L))).toBeTrue();
    }
}