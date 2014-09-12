package com.soundcloud.android.playback.ui;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.tracks.TrackUrn;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.widget.ImageView;

import javax.inject.Inject;

public class PlayerArtworkLoader {

    protected final ImageOperations imageOperations;
    protected final Resources resources;

    @Inject
    public PlayerArtworkLoader(ImageOperations imageOperations, Resources resources) {
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    public void loadArtwork(TrackUrn urn, ImageView wrappedImageView, ImageView imageOverlay, ImageListener listener, boolean isHighPriority,
                            ViewVisibilityProvider viewVisibilityProvider){
        final ApiImageSize size = ApiImageSize.getFullImageSize(resources);
        final Bitmap cachedListBitmap = imageOperations.getCachedListItemBitmap(resources, urn);
        imageOperations.displayInPlayer(urn, size, wrappedImageView, listener, cachedListBitmap, isHighPriority);
    }
}
