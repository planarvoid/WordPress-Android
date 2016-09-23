package com.soundcloud.android.image;

import com.soundcloud.java.optional.Optional;

import android.graphics.Bitmap;

public interface ImageProcessor {
    Bitmap blurBitmap(Bitmap bitmap, Optional<Float> blurRadius);
    Bitmap blurBitmap(Bitmap inBitmap, Bitmap outBitmap, Optional<Float> blurRadius);
}
