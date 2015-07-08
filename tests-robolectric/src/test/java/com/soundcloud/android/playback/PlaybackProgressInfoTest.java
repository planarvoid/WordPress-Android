package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.playback.PlaybackProgressInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.model.Urn;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackProgressInfoTest {

    @Test
    public void shouldResumeTrackReturnsFalseForDifferentTrack() throws Exception {
        expect(new PlaybackProgressInfo(1L, 100L).shouldResumeTrack(Urn.forTrack(2L))).toBeFalse();
    }

    @Test
    public void shouldResumeTrackReturnsFalseWithoutProgress() throws Exception {
        expect(new PlaybackProgressInfo(1L, 0L).shouldResumeTrack(Urn.forTrack(1L))).toBeFalse();
    }

    @Test
    public void shouldResumeTrackReturnsTrueWithoutPositiveProgressForSameTrack() throws Exception {
        expect(new PlaybackProgressInfo(1L, 10L).shouldResumeTrack(Urn.forTrack(1L))).toBeTrue();
    }
}