package com.soundcloud.android.playback.ui;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.widget.ImageView;

@RunWith(SoundCloudTestRunner.class)
public class PlayerArtworkLoaderTest {

    @Mock ImageOperations imageOperations;
    @Mock ImageView wrappedImageView;
    @Mock ImageView imageOverlayView;
    @Mock ImageListener listener;
    @Mock ViewVisibilityProvider viewVisibilityProvider;

    private PlayerArtworkLoader playerArtworkLoader;
    private Resources resources = Robolectric.application.getResources();


    @Before
    public void setUp() throws Exception {
        playerArtworkLoader = new PlayerArtworkLoader(imageOperations, Robolectric.application.getResources());
    }

    @Test
    public void loadArtworkLoadsArtworkThroughImageOperations() throws Exception {
        final Urn urn = Urn.forTrack(123L);
        final Bitmap cachedBitmap = Bitmap.createBitmap(0,0, Bitmap.Config.RGB_565);
        when(imageOperations.getCachedListItemBitmap(resources, urn)).thenReturn(cachedBitmap);

        playerArtworkLoader.loadArtwork(urn, wrappedImageView, imageOverlayView, true, viewVisibilityProvider);

        verify(imageOperations).displayInPlayer(urn, ApiImageSize.getFullImageSize(Robolectric.application.getResources()),
                wrappedImageView, cachedBitmap, true);
    }
}