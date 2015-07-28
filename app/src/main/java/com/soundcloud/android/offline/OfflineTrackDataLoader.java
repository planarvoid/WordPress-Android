package com.soundcloud.android.offline;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.waveform.WaveformFetchCommand;

import android.content.res.Resources;

import javax.inject.Inject;

class OfflineTrackDataLoader {

    private final ImageOperations imageOperations;
    private final Resources resources;
    private final WaveformFetchCommand waveformFetchCommand;
    private final WaveformStorage waveformStorage;

    @Inject
    public OfflineTrackDataLoader(ImageOperations imageOperations, Resources resources, WaveformFetchCommand waveformFetchCommand, WaveformStorage waveformStorage) {
        this.waveformFetchCommand = waveformFetchCommand;
        this.waveformStorage = waveformStorage;
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    public void fetchTrackArtwork(Urn trackUrn) {
        Log.d(OfflineContentService.TAG, "Prefetch artwork called for: " + trackUrn);
        final ApiImageSize playerSize = ApiImageSize.getFullImageSize(resources);
        imageOperations.precacheTrackArtwork(trackUrn, playerSize);

        final ApiImageSize listItemSize = ApiImageSize.getListItemImageSize(resources);
        imageOperations.precacheTrackArtwork(trackUrn, listItemSize);
    }

    public void fetchTrackWaveform(Urn trackUrn) {
        if (!waveformStorage.hasWaveform(trackUrn)) {
            try {
                waveformStorage.store(trackUrn, waveformFetchCommand.call("http"));
            } catch (WaveformFetchCommand.WaveformFetchException ignored) {
                // default waveform will be displayed
            }
        }
    }

}
