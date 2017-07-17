package com.soundcloud.android.image;

import com.soundcloud.android.utils.images.ImageUtils;
import rx.Subscriber;

import android.graphics.Bitmap;
import android.view.View;

import javax.inject.Inject;

public class FallbackBitmapLoadingAdapter extends ImageUtils.ViewlessLoadingListener {

    private final Subscriber<? super Bitmap> subscriber;
    private final Bitmap fallbackImage;

    public FallbackBitmapLoadingAdapter(Subscriber<? super Bitmap> subscriber, Bitmap fallbackImage) {
        this.subscriber = subscriber;
        this.fallbackImage = fallbackImage;
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        if (subscriber.isUnsubscribed()) {
            fallbackImage.recycle();
        } else {
            emitLoadedOrFallbackImage(loadedImage);
        }
    }

    private void emitLoadedOrFallbackImage(Bitmap loadedImage) {
        if (loadedImage == null) {
            // Sometimes image loader emit a null bitmap which is considered as an error here.
            emitAndComplete(fallbackImage);
        } else {
            emitAndComplete(loadedImage);
            fallbackImage.recycle();
        }
    }

    @Override
    public void onLoadingFailed(String imageUri, View view, Throwable cause) {
        if (!subscriber.isUnsubscribed()) {
            emitAndComplete(fallbackImage);
        }
    }

    private void emitAndComplete(Bitmap image) {
        if (!subscriber.isUnsubscribed()) {
            subscriber.onNext(image);
            subscriber.onCompleted();
        }
    }

    public static class Factory {

        @Inject
        public Factory() {
            // no-op
        }

        public FallbackBitmapLoadingAdapter create(Subscriber subscriber, Bitmap fallback) {
            return new FallbackBitmapLoadingAdapter(subscriber, fallback);
        }
    }
}
