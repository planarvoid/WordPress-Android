package com.soundcloud.android.utils.images;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(SoundCloudTestRunner.class)
public class ImageUtilsTest {

    @Test
    public void getCachedTrackListIconShouldReturnNullWithNoArtwork(){
        Track track = Mockito.mock(Track.class);
        when(track.getAvatarUrl()).thenReturn(null);
        expect(ImageUtils.getCachedTrackListIcon(Robolectric.application, track)).toBeNull();
    }
}
