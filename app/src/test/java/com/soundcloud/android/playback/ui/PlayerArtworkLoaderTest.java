package com.soundcloud.android.playback.ui;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.graphics.Bitmap;
import android.widget.ImageView;

public class PlayerArtworkLoaderTest extends AndroidUnitTest {

    @Mock ImageOperations imageOperations;
    @Mock ImageView wrappedImageView;
    @Mock ImageView imageOverlayView;

    private PlayerArtworkLoader playerArtworkLoader;
    private Urn urn = Urn.forTrack(1L);
    private ImageResource imageResource = SimpleImageResource.create(urn, Optional.absent());

    @Before
    public void setUp() throws Exception {
        playerArtworkLoader = new PlayerArtworkLoader(imageOperations, resources());
    }

    @Test
    public void loadArtworkLoadsArtworkThroughImageOperations() {
        final Bitmap cachedBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
        when(imageOperations.getCachedListItemBitmap(resources(), urn, Optional.absent())).thenReturn(cachedBitmap);

        playerArtworkLoader.loadArtwork(imageResource,
                                        wrappedImageView,
                                        imageOverlayView,
                                        true);

        verify(imageOperations).displayInPlayer(urn, Optional.absent(), ApiImageSize.getFullImageSize(resources()), wrappedImageView, cachedBitmap, true);
    }
}
