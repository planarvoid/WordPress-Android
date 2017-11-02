package com.soundcloud.android.playback.ui;

import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import rx.Observable;
import rx.Subscription;

import android.content.res.Resources;
import android.graphics.Bitmap;
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
        return RxJava.toV1Observable(imageOperations.blurredArtwork(resources,
                                                                    trackUrn,
                                                                    Optional.absent(),
                                                                    Optional.absent(),
                                                                    graphicsScheduler,
                                                                    observeOnScheduler));
    }

    @Override
    public void loadArtwork(ImageResource imageResource, ImageView wrappedImageView, ImageView imageOverlay, boolean isHighPriority) {
        super.loadArtwork(imageResource, wrappedImageView, imageOverlay, isHighPriority);
        loadBlurredArtwork(imageResource, imageOverlay);
    }

    protected void loadBlurredArtwork(ImageResource imageResource, ImageView imageOverlay) {
        blurSubscription.unsubscribe();
        blurSubscription = RxJava.toV1Observable(imageOperations.blurredArtwork(resources,
                                                                                imageResource.getUrn(),
                                                                                imageResource.getImageUrlTemplate(),
                                                                                Optional.absent(),
                                                                                graphicsScheduler,
                                                                                observeOnScheduler))
                                 .subscribe(new BlurredOverlaySubscriber(imageOverlay));
    }

    private class BlurredOverlaySubscriber extends DefaultSubscriber<Bitmap> {
        private final WeakReference<ImageView> imageOverlayRef;

        BlurredOverlaySubscriber(ImageView imageOverlay) {
            this.imageOverlayRef = new WeakReference<>(imageOverlay);
        }

        @Override
        public void onNext(Bitmap bitmap) {
            final ImageView imageView = imageOverlayRef.get();

            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}
