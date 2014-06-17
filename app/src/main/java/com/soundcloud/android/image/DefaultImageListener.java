package com.soundcloud.android.image;


import android.graphics.Bitmap;
import android.view.View;

public abstract class DefaultImageListener implements ImageListener {
    @Override
    public void onLoadingStarted(String imageUri, View view) {
        // no-op
    }

    @Override
    public void onLoadingFailed(String imageUri, View view, String failedReason) {
        // no-op
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        // no-op
    }
}
