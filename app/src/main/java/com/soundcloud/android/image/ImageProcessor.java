package com.soundcloud.android.image;

import android.content.Context;
import android.graphics.Bitmap;

public interface ImageProcessor {
    public Bitmap blurBitmap(Bitmap bitmap);
}
