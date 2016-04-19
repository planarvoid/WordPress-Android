package com.soundcloud.android.playback.ui;

import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.images.ImageUtils;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

public class BlurringPlayerArtworkLoader extends PlayerArtworkLoader {

    private final Scheduler graphicsScheduler;
    private final Scheduler observeOnScheduler;

    private Subscription blurSubscription = RxUtils.invalidSubscription();


    public BlurringPlayerArtworkLoader(ImageOperations imageOperations, Resources resources,
                                       Scheduler graphicsScheduler) {
        this(imageOperations, resources, graphicsScheduler, AndroidSchedulers.mainThread());
    }

    public BlurringPlayerArtworkLoader(ImageOperations imageOperations, Resources resources,
                                       Scheduler graphicsScheduler, Scheduler observeOnScheduler) {
        super(imageOperations, resources);
        this.graphicsScheduler = graphicsScheduler;
        this.observeOnScheduler = observeOnScheduler;
    }

    @Override
    public void loadArtwork(ImageResource imageResource, ImageView wrappedImageView, ImageView imageOverlay,
                            boolean isHighPriority, ViewVisibilityProvider viewVisibilityProvider) {

        super.loadArtwork(imageResource, wrappedImageView, imageOverlay, isHighPriority, viewVisibilityProvider);

        blurSubscription.unsubscribe();
        blurSubscription = imageOperations.blurredPlayerArtwork(resources, imageResource, graphicsScheduler, observeOnScheduler)
                .subscribe(new BlurredOverlaySubscriber(imageOverlay, viewVisibilityProvider));
    }

    private class BlurredOverlaySubscriber extends DefaultSubscriber<Bitmap> {
        private final WeakReference<ImageView> imageOverlayRef;
        private final ViewVisibilityProvider viewVisibilityProvider;

        public BlurredOverlaySubscriber(ImageView imageOverlay, ViewVisibilityProvider viewVisibilityProvider) {
            this.imageOverlayRef = new WeakReference<>(imageOverlay);
            this.viewVisibilityProvider = viewVisibilityProvider;
        }

        @Override
        public void onNext(Bitmap bitmap) {
            final ImageView imageView = imageOverlayRef.get();

            if (imageView != null) {
                if (viewVisibilityProvider != null && viewVisibilityProvider.isCurrentlyVisible(imageView)) {
                    final TransitionDrawable transitionDrawable = ImageUtils.createTransitionDrawable(null, new BitmapDrawable(bitmap));
                    imageView.setImageDrawable(transitionDrawable);
                    transitionDrawable.startTransition(ImageUtils.DEFAULT_TRANSITION_DURATION);
                } else {
                    // we are being extremely defensive around a release bug. Remove this and null check if it does not appear
                    if (viewVisibilityProvider == null) {
                        ErrorUtils.handleSilentException(new IllegalStateException("View Visibility Provider null in Blurring artwork loader"));
                    }
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }
}
