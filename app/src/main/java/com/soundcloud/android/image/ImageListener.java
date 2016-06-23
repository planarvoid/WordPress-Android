package com.soundcloud.android.image;

import android.graphics.Bitmap;
import android.view.View;

public interface ImageListener {
    void onLoadingStarted(String imageUri, View view);

    void onLoadingFailed(String imageUri, View view, String failedReason);

    void onLoadingComplete(String imageUri, View view, Bitmap loadedImage);
}
