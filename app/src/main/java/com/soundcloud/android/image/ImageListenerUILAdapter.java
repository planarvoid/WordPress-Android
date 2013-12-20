package com.soundcloud.android.image;

import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

import android.graphics.Bitmap;
import android.view.View;

public class ImageListenerUILAdapter implements ImageLoadingListener {

    private ImageListener mImageListener;

    public ImageListenerUILAdapter(ImageListener imageListener) {
        mImageListener = imageListener;
    }

    @Override
    public void onLoadingStarted(String imageUri, View view) {
        mImageListener.onLoadingStarted(imageUri, view);
    }

    @Override
    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
        mImageListener.onLoadingFailed(imageUri, view,
                failReason != null && failReason.getCause() != null ? failReason.getCause().getMessage() : null);
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap bitmap) {
        mImageListener.onLoadingComplete(imageUri, view, bitmap);
    }

    @Override
    public void onLoadingCancelled(String imageUri, View view) {
        // No implementation
    }
}
