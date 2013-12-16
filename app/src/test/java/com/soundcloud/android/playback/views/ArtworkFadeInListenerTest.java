package com.soundcloud.android.playback.views;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.view.animation.Animation;

@RunWith(SoundCloudTestRunner.class)
public class ArtworkFadeInListenerTest {

    ArtworkFadeInListener artworkFadeInListener;
    @Mock
    ArtworkTrackView playerArtworkTrackView;

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
