package com.soundcloud.android.image;

import com.soundcloud.java.optional.Optional;

import android.graphics.Bitmap;

public class ImageProcessorCompat implements ImageProcessor {
    @Override
    public Bitmap blurBitmap(Bitmap bitmap, Optional<Float> blurRadius) {
        return bitmap;
    }

    @Override
    public Bitmap blurBitmap(Bitmap inBitmap, Bitmap outBitmap, Optional<Float> blurRadius) {
        return inBitmap;
    }

}
