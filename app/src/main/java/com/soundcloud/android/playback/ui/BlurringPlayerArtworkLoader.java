package com.soundcloud.android.playback.ui;

import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackUrn;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

public class BlurringPlayerArtworkLoader extends PlayerArtworkLoader {

    private final Scheduler graphicsScheduler;

    private Subscription blurSubscription = Subscriptions.empty();

    public BlurringPlayerArtworkLoader(ImageOperations imageOperations, Resources resources,
                                       Scheduler graphicsScheduler) {
        super(imageOperations, resources);
        this.graphicsScheduler = graphicsScheduler;
    }

    @Override
    public void loadArtwork(TrackUrn urn, ImageView wrappedImageView, ImageView imageOverlay, ImageListener listener, boolean isHighPriority) {
        super.loadArtwork(urn, wrappedImageView, imageOverlay, listener, isHighPriority);

        blurSubscription.unsubscribe();
        blurSubscription = imageOperations.blurredPlayerArtwork(resources, urn)
                .subscribeOn(graphicsScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BlurredOverlaySubscriber(imageOverlay));
    }

    private class BlurredOverlaySubscriber extends DefaultSubscriber<Bitmap> {
        private final WeakReference<ImageView> imageOverlayRef;

        public BlurredOverlaySubscriber(ImageView imageOverlay) {
            this.imageOverlayRef = new WeakReference<ImageView>(imageOverlay);
        }

        @Override
        public void onNext(Bitmap bitmap) {
            ImageView imageView = imageOverlayRef.get();
            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}
