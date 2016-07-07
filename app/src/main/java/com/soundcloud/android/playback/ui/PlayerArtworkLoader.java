package com.soundcloud.android.playback.ui;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import rx.Observable;

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

    public void loadArtwork(ImageResource imageResource,
                            ImageView wrappedImageView,
                            ImageView imageOverlay,
                            boolean isHighPriority,
                            ViewVisibilityProvider viewVisibilityProvider) {
        final ApiImageSize size = ApiImageSize.getFullImageSize(resources);
        final Bitmap cachedListBitmap = imageOperations.getCachedListItemBitmap(resources, imageResource);
        imageOperations.displayInPlayer(imageResource, size, wrappedImageView, cachedListBitmap, isHighPriority);
    }

    public Observable<Bitmap> loadAdBackgroundImage(Urn trackUrn) {
        return imageOperations.bitmap(toImageResource(trackUrn), ApiImageSize.getFullImageSize(resources));
    }

    // Artwork loaded from a play queue item does not have an ImageResource
    protected static ImageResource toImageResource(final Urn trackUrn) {
        return new ImageResource() {
            @Override
            public Urn getUrn() {
                return trackUrn;
            }
            @Override
            public Optional<String> getImageUrlTemplate() {
                return Optional.absent();
            }
        };
    }

}
