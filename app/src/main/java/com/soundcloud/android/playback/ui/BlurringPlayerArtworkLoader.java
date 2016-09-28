package com.soundcloud.android.playback.ui;

import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
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
    public Observable<Bitmap> loadAdBackgroundImage(final Urn trackUrn) {
        return imageOperations.blurredPlayerArtwork(resources,
                                                    toImageResource(trackUrn),
                                                    graphicsScheduler,
                                                    observeOnScheduler);
    }

    @Override
    public void loadArtwork(ImageResource imageResource, ImageView wrappedImageView, ImageView imageOverlay,
                            boolean isHighPriority, ViewVisibilityProvider viewVisibilityProvider) {
        super.loadArtwork(imageResource, wrappedImageView, imageOverlay, isHighPriority, viewVisibilityProvider);
        loadBlurredArtwork(imageResource, imageOverlay, Optional.of(viewVisibilityProvider));
    }

    protected void loadBlurredArtwork(ImageResource imageResource,
                                      ImageView imageOverlay,
                                      Optional<ViewVisibilityProvider> viewVisibilityProvider) {
        blurSubscription.unsubscribe();
        blurSubscription = imageOperations.blurredPlayerArtwork(resources,
                                                                imageResource,
                                                                graphicsScheduler,
                                                                observeOnScheduler)
                                          .subscribe(new BlurredOverlaySubscriber(imageOverlay,
                                                                                  viewVisibilityProvider));
    }

    private class BlurredOverlaySubscriber extends DefaultSubscriber<Bitmap> {
        private final WeakReference<ImageView> imageOverlayRef;
        private final Optional<ViewVisibilityProvider> viewVisibilityProvider;

        BlurredOverlaySubscriber(ImageView imageOverlay, Optional<ViewVisibilityProvider> viewVisibilityProvider) {
            this.imageOverlayRef = new WeakReference<>(imageOverlay);
            this.viewVisibilityProvider = viewVisibilityProvider;
        }

        @Override
        public void onNext(Bitmap bitmap) {
            final ImageView imageView = imageOverlayRef.get();

            if (imageView != null) {
                if (viewVisibilityProvider.isPresent() && viewVisibilityProvider.get().isCurrentlyVisible(imageView)) {
                    final TransitionDrawable transitionDrawable =
                            ImageUtils.createTransitionDrawable(null, new BitmapDrawable(bitmap));
                    imageView.setImageDrawable(transitionDrawable);
                    transitionDrawable.startTransition(ImageUtils.DEFAULT_TRANSITION_DURATION);
                } else {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }
}
