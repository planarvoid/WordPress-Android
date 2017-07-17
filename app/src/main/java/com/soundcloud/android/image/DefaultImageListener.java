package com.soundcloud.android.image;

import android.graphics.Bitmap;
import android.view.View;

public class DefaultImageListener implements ImageListener {
    @Override
    public void onLoadingStarted(String imageUri, View view) {
    }

    @Override
    public void onLoadingFailed(String imageUri, View view, Throwable cause) {

    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
    }
}
