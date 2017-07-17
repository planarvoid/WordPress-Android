package com.soundcloud.android.image;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.view.View;

public interface ImageListener {
    void onLoadingStarted(String imageUri, View view);

    void onLoadingFailed(String imageUri, View view, @Nullable Throwable cause);

    void onLoadingComplete(String imageUri, View view, Bitmap loadedImage);
}
