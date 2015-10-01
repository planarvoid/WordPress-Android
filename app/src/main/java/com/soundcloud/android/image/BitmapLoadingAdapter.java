package com.soundcloud.android.image;

import com.soundcloud.android.utils.images.ImageUtils;
import rx.Subscriber;

import android.graphics.Bitmap;
import android.view.View;

import javax.inject.Inject;

public class BitmapLoadingAdapter extends ImageUtils.ViewlessLoadingListener {

    private final Subscriber<? super Bitmap> subscriber;

    public BitmapLoadingAdapter(Subscriber<? super Bitmap> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        if (subscriber.isUnsubscribed()) {
            loadedImage.recycle();
        } else {
            if (loadedImage != null) {
                subscriber.onNext(loadedImage);
                subscriber.onCompleted();
            }
        }
    }

    @Override
    public void onLoadingFailed(String imageUri, View view, String failedReason) {
        // No-op
    }

    public static class Factory {
        @Inject
        public Factory() {
            // No-op
        }

        public BitmapLoadingAdapter create(Subscriber<? super Bitmap> subscriber) {
            return new BitmapLoadingAdapter(subscriber);
        }
    }
}
