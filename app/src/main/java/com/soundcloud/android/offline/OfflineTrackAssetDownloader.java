package com.soundcloud.android.offline;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.waveform.WaveformStorage;
import com.soundcloud.android.waveform.WaveformFetchCommand;

import android.content.res.Resources;

import javax.inject.Inject;

class OfflineTrackAssetDownloader {

    private final ImageOperations imageOperations;
    private final Resources resources;
    private final WaveformFetchCommand waveformFetchCommand;
    private final WaveformStorage waveformStorage;

    @Inject
    OfflineTrackAssetDownloader(ImageOperations imageOperations,
                                Resources resources,
                                WaveformFetchCommand waveformFetchCommand,
                                WaveformStorage waveformStorage) {
        this.waveformFetchCommand = waveformFetchCommand;
        this.waveformStorage = waveformStorage;
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    void fetchTrackArtwork(ImageResource imageResource) {
        Log.d(OfflineContentService.TAG, "Prefetch artwork called for: " + imageResource);
        final ApiImageSize playerSize = ApiImageSize.getFullImageSize(resources);
        imageOperations.precacheArtwork(imageResource.getUrn(), imageResource.getImageUrlTemplate(), playerSize);

        final ApiImageSize listItemSize = ApiImageSize.getListItemImageSize(resources);
        imageOperations.precacheArtwork(imageResource.getUrn(), imageResource.getImageUrlTemplate(), listItemSize);
    }

    void fetchTrackWaveform(Urn trackUrn, String waveformUrl) {
        Log.d(OfflineContentService.TAG, "Prefetch waveform called for: " + trackUrn);
        if (!waveformStorage.isWaveformStored(trackUrn)) {
            try {
                waveformStorage.store(trackUrn, waveformFetchCommand.call(waveformUrl));
            } catch (WaveformFetchCommand.WaveformFetchException exception) {
                // Default waveform will be displayed
                ErrorUtils.handleSilentException("Failed to download waveform for track: " + trackUrn, exception);
            }
        }
    }

}
