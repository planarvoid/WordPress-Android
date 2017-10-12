package com.soundcloud.android.image;

import com.soundcloud.android.utils.images.ImageUtils;
import io.reactivex.SingleEmitter;

import android.graphics.Bitmap;
import android.view.View;

import javax.inject.Inject;

public class BitmapLoadingAdapter extends ImageUtils.ViewlessLoadingListener {

    private final SingleEmitter<? super Bitmap> subscriber;

    public BitmapLoadingAdapter(SingleEmitter<? super Bitmap> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onLoadingFailed(String imageUri, View view, Throwable cause) {
        subscriber.onError(new BitmapLoadingException(cause));
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        subscriber.onSuccess(loadedImage);
    }

    public static class Factory {
        @Inject
        Factory() {
            // No-op
        }

        public BitmapLoadingAdapter create(SingleEmitter<? super Bitmap> subscriber) {
            return new BitmapLoadingAdapter(subscriber);
        }
    }

    public static class BitmapLoadingException extends Exception {

        public BitmapLoadingException(Throwable cause) {
            super(cause);
        }
    }
}
