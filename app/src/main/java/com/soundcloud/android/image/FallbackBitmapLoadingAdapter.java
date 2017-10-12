package com.soundcloud.android.image;

import com.soundcloud.android.utils.images.ImageUtils;
import io.reactivex.SingleEmitter;

import android.graphics.Bitmap;
import android.view.View;

import javax.inject.Inject;

public class FallbackBitmapLoadingAdapter extends ImageUtils.ViewlessLoadingListener {

    private final SingleEmitter<? super Bitmap> subscriber;
    private final Bitmap fallbackImage;

    public FallbackBitmapLoadingAdapter(SingleEmitter<? super Bitmap> subscriber, Bitmap fallbackImage) {
        this.subscriber = subscriber;
        this.fallbackImage = fallbackImage;
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        if (subscriber.isDisposed()) {
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
        if (!subscriber.isDisposed()) {
            emitAndComplete(fallbackImage);
        }
    }

    private void emitAndComplete(Bitmap image) {
        subscriber.onSuccess(image);
    }

    public static class Factory {

        @Inject
        public Factory() {
            // no-op
        }

        public FallbackBitmapLoadingAdapter create(SingleEmitter<? super Bitmap> subscriber, Bitmap fallback) {
            return new FallbackBitmapLoadingAdapter(subscriber, fallback);
        }
    }
}
