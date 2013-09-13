package com.soundcloud.android.view.play;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.ImageLoaderUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.graphics.Bitmap;
import android.view.animation.Animation;
import android.widget.ImageView;

@RunWith(SoundCloudTestRunner.class)
public class ArtworkFadeInListenerTest {

    ArtworkFadeInListener artworkFadeInListener;
    @Mock
    PlayerArtworkTrackView playerArtworkTrackView;

    @Before
    public void setUp() throws Exception {
        artworkFadeInListener = new ArtworkFadeInListener(playerArtworkTrackView);
    }

    @Test
    public void shouldClearTrackViewBackgroundOnComplete() throws Exception {
        final Animation animation = Mockito.mock(Animation.class);
        artworkFadeInListener.onAnimationEnd(animation);
        verify(playerArtworkTrackView).clearBackgroundAfterAnimation(animation);
    }
}
