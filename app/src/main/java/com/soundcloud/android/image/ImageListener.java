package com.soundcloud.android.image;

import android.graphics.Bitmap;
import android.view.View;

public interface ImageListener {
    public void onLoadingStarted(String imageUri, View view);
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage);
}
