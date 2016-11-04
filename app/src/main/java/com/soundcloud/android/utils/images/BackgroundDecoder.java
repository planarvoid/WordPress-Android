package com.soundcloud.android.utils.images;

import com.soundcloud.android.utils.DeviceHelper;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.DrawableRes;

import javax.inject.Inject;

public class BackgroundDecoder {

    private static final int SAMPLE_SIZE = 2;

    private final Resources resources;
    private final DeviceHelper deviceHelper;

    @Inject
    public BackgroundDecoder(Resources resources, DeviceHelper deviceHelper) {
        this.resources = resources;
        this.deviceHelper = deviceHelper;
    }

    public Bitmap decode(@DrawableRes int imageResource) {
        BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
        if (deviceHelper.isLowMemoryDevice() || shouldResampleBackground(imageResource)) {
            decodeOpts.inSampleSize = SAMPLE_SIZE;
        }
        return BitmapFactory.decodeResource(resources, imageResource, decodeOpts);
    }

    // Resample if screen width is closer to sampled width than source width
    private boolean shouldResampleBackground(@DrawableRes int imageResource) {
        BitmapFactory.Options measureOpts = new BitmapFactory.Options();
        measureOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, imageResource, measureOpts);
        int resampleWidth = measureOpts.outWidth / SAMPLE_SIZE;
        int screenWidth = deviceHelper.getDisplayMetrics().widthPixels;
        return Math.abs(measureOpts.outWidth - screenWidth) > Math.abs(resampleWidth - screenWidth);
    }

}
