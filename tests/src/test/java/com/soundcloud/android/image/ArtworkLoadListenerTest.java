package com.soundcloud.android.image;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.views.ArtworkTrackView;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.graphics.Bitmap;
import android.widget.ImageView;

@RunWith(SoundCloudTestRunner.class)
public class ArtworkLoadListenerTest {

    ArtworkLoadListener artworkLoadListener;
    @Mock
    ArtworkTrackView playerArtworkTrackView;
    @Mock
    Track track;
    @Mock
    ImageLoaderUtils imageLoaderUtils;
    @Mock
    ImageView imageView;

    @Before
    public void setUp() throws Exception {
        artworkLoadListener = new ArtworkLoadListener(playerArtworkTrackView, track, imageLoaderUtils);
    }

    @Test
    public void shouldNotSetBitmapWithNoCachedBitmap() throws Exception {
        artworkLoadListener.onLoadingStarted("artworkUri", imageView);
        verifyZeroInteractions(playerArtworkTrackView);
    }

    @Test
    public void shouldSetBitmapWithCachedBitmap() throws Exception {
        final Bitmap bitmap = Mockito.mock(Bitmap.class);
        when(imageLoaderUtils.getCachedTrackListIcon(track)).thenReturn(bitmap);
        artworkLoadListener.onLoadingStarted("artworkUri", imageView);
        verify(playerArtworkTrackView).setTemporaryArtwork(bitmap);
    }

    @Test
    public void shouldCallOnArtworkSetWithAnimateOnComplete() throws Exception {
        artworkLoadListener.onLoadingComplete("artworkUri", imageView, Mockito.mock(Bitmap.class));
        verify(playerArtworkTrackView).onArtworkSet(true);
    }
}
