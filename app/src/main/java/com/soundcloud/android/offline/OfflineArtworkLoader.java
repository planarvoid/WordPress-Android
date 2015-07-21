package com.soundcloud.android.offline;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Log;

import android.content.res.Resources;

import javax.inject.Inject;

class OfflineArtworkLoader {

    private final ImageOperations imageOperations;
    private final Resources resources;

    @Inject
    public OfflineArtworkLoader(ImageOperations imageOperations, Resources resources) {
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
}
