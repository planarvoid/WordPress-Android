package com.soundcloud.android.image;

import com.soundcloud.android.utils.images.ImageUtils;
import rx.Subscriber;

import android.graphics.Bitmap;
import android.view.View;

import javax.inject.Inject;

public class ViewlessLoadingAdapter extends ImageUtils.ViewlessLoadingListener {

    private final Subscriber<? super Bitmap> subscriber;
    private final boolean copyBeforeDelivery;

    public ViewlessLoadingAdapter(Subscriber<? super Bitmap> subscriber, boolean copyBeforeDelivery) {
        this.subscriber = subscriber;
        this.copyBeforeDelivery = copyBeforeDelivery;
    }

    @Override
    public void onLoadingFailed(String s, View view, String failedReason) {
        subscriber.onError(new Exception("Failed to load bitmap " + failedReason));
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        if (!subscriber.isUnsubscribed()) {
            try {
                if (copyBeforeDelivery){
                    loadedImage = loadedImage.copy(Bitmap.Config.ARGB_8888, false);
                }
                subscriber.onNext(loadedImage);

            } catch (OutOfMemoryError error) {
                onLoadingFailed(imageUri, view, error.toString());
            }
            subscriber.onCompleted();
        }
    }

    public static class Factory {

        @Inject
        public Factory() {
        }

        public ViewlessLoadingAdapter create(Subscriber subscriber, boolean copyBeforeDelivery){
            return new ViewlessLoadingAdapter(subscriber, copyBeforeDelivery);
        }
    }
}
