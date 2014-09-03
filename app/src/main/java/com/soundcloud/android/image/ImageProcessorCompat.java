package com.soundcloud.android.image;

import android.graphics.Bitmap;

public class ImageProcessorCompat implements ImageProcessor {
    @Override
    public Bitmap blurBitmap(Bitmap bitmap) {
        return bitmap;
    }
}
