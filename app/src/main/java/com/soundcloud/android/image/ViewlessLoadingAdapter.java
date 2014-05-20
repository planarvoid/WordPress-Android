package com.soundcloud.android.image;

import com.soundcloud.android.utils.images.ImageUtils;
import rx.Subscriber;

import android.graphics.Bitmap;
import android.view.View;

import javax.inject.Inject;
import java.io.IOException;

public class ViewlessLoadingAdapter extends ImageUtils.ViewlessLoadingListener {

    private final Subscriber<? super Bitmap> subscriber;
    private final boolean copyBeforeDelivery;

    public ViewlessLoadingAdapter(Subscriber<? super Bitmap> subscriber, boolean copyBeforeDelivery) {
        this.subscriber = subscriber;
        this.copyBeforeDelivery = copyBeforeDelivery;
    }

    @Override
    public void onLoadingFailed(String s, View view, String failedReason) {
        subscriber.onError(new IOException("Failed to load bitmap " + failedReason));
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        if (!subscriber.isUnsubscribed()) {
            try {
                subscriber.onNext(copyBeforeDelivery ? loadedImage.copy(Bitmap.Config.ARGB_8888, false) : loadedImage);

            } catch (OutOfMemoryError error) {
                onLoadingFailed(imageUri, view, error.toString());
            }
            subscriber.onCompleted();
        }
    }

    public static class Factory {

        @Inject
        public Factory() {
            // no-op
        }

        public ViewlessLoadingAdapter create(Subscriber subscriber, boolean copyBeforeDelivery){
            return new ViewlessLoadingAdapter(subscriber, copyBeforeDelivery);
        }
    }
}
