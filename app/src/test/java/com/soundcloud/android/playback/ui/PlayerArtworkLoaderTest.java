package com.soundcloud.android.playback.ui;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.graphics.Bitmap;
import android.widget.ImageView;

public class PlayerArtworkLoaderTest extends AndroidUnitTest {

    @Mock ImageOperations imageOperations;
    @Mock ImageView wrappedImageView;
    @Mock ImageView imageOverlayView;
    @Mock ImageListener listener;
    @Mock ViewVisibilityProvider viewVisibilityProvider;

    private PlayerArtworkLoader playerArtworkLoader;

    @Before
    public void setUp() throws Exception {
        playerArtworkLoader = new PlayerArtworkLoader(imageOperations, resources());
    }

    @Test
    public void loadArtworkLoadsArtworkThroughImageOperations() {
        final Urn urn = Urn.forTrack(123L);
        final Bitmap cachedBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
        when(imageOperations.getCachedListItemBitmap(resources(), urn)).thenReturn(cachedBitmap);

        playerArtworkLoader.loadArtwork(urn, wrappedImageView, imageOverlayView, true, viewVisibilityProvider);

        verify(imageOperations).displayInPlayer(urn, ApiImageSize.getFullImageSize(resources()),
                wrappedImageView, cachedBitmap, true);
    }
}
