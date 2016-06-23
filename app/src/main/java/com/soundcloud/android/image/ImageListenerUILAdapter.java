package com.soundcloud.android.image;

import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import android.graphics.Bitmap;
import android.view.View;

public class ImageListenerUILAdapter implements ImageLoadingListener {

    private final ImageListener imageListener;

    public ImageListenerUILAdapter(ImageListener imageListener) {
        this.imageListener = imageListener;
    }

    @Override
    public void onLoadingStarted(String imageUri, View view) {
        imageListener.onLoadingStarted(imageUri, view);
    }

    @Override
    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
        imageListener.onLoadingFailed(imageUri, view,
                                      failReason != null && failReason.getCause() != null ?
                                      failReason.getCause().getMessage() :
                                      null);
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap bitmap) {
        imageListener.onLoadingComplete(imageUri, view, bitmap);
    }

    @Override
    public void onLoadingCancelled(String imageUri, View view) {
        // No implementation
    }
}
