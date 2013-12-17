package com.soundcloud.android.image;

import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

import android.graphics.Bitmap;
import android.view.View;

public class ImageListenerUILAdapter implements ImageLoadingListener {

    ImageListener mImageListener;

    public ImageListenerUILAdapter(ImageListener imageListener) {
        mImageListener = imageListener;
    }

    @Override
    public void onLoadingStarted(String s, View view) {
        mImageListener.onLoadingStarted(s, view);
    }

    @Override
    public void onLoadingFailed(String s, View view, FailReason failReason) {
        mImageListener.onLoadingFailed(s, view, failReason.getCause().getMessage());
    }

    @Override
    public void onLoadingComplete(String s, View view, Bitmap bitmap) {
        mImageListener.onLoadingComplete(s, view, bitmap);
    }

    @Override
    public void onLoadingCancelled(String s, View view) {
        // No implementation
    }
}
