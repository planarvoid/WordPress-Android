package com.soundcloud.android.offline;


import static org.mockito.Mockito.verify;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class OfflineArtworkLoaderTest extends AndroidUnitTest {

    @Mock ImageOperations imageOperations;

    private OfflineArtworkLoader loader;
    private Urn track = Urn.forTrack(123L);

    @Before
    public void setUp() {
        loader = new OfflineArtworkLoader(imageOperations, resources());
    }

    @Test
    public void prefetchCachesArtworkInFullImageSize() {
        final ApiImageSize size = ApiImageSize.getFullImageSize(resources());

        loader.fetchTrackArtwork(track);

        verify(imageOperations).precacheTrackArtwork(track, size);
    }

    @Test
    public void prefetchCachesArtworkInListItemSize() {
        final ApiImageSize size = ApiImageSize.getListItemImageSize(resources());

        loader.fetchTrackArtwork(track);

        verify(imageOperations).precacheTrackArtwork(track, size);
    }

}
