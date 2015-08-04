package com.soundcloud.android.offline;


import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.waveform.WaveformData;
import com.soundcloud.android.waveform.WaveformFetchCommand;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class OfflineTrackAssetDownloaderTest extends AndroidUnitTest {

    @Mock ImageOperations imageOperations;
    @Mock WaveformFetchCommand waveformFetchCommand;
    @Mock WaveformStorage waveformStorage;

    private final Urn track = Urn.forTrack(123L);
    private final String waveformUrl = "http://wav";

    private OfflineTrackAssetDownloader loader;

    @Before
    public void setUp() {
        loader = new OfflineTrackAssetDownloader(imageOperations, resources(), waveformFetchCommand, waveformStorage);
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

    @Test
    public void prefetchDoesNotFetchAndStoreWaveformIfAlreadyExist() {
        when(waveformStorage.hasWaveform(track)).thenReturn(true);

        loader.fetchTrackWaveform(track, waveformUrl);

        verifyZeroInteractions(waveformFetchCommand);
        verify(waveformStorage, never()).store(any(Urn.class), any(WaveformData.class));
    }

    @Test
    public void prefetchFetchesAndStoresWaveform() {
        WaveformData data = new WaveformData(12, new int[]{12});
        when(waveformFetchCommand.call(waveformUrl)).thenReturn(data);

        loader.fetchTrackWaveform(track, waveformUrl);

        verify(waveformStorage).store(track, data);
    }
}
