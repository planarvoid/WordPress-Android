package com.soundcloud.android.image;

import com.soundcloud.android.lightcycle.DefaultActivityLightCycle;

import android.support.v4.app.FragmentActivity;

import javax.inject.Inject;

public class ImageOperationsLightCycle extends DefaultActivityLightCycle {
    private final ImageOperations imageOperations;

    @Inject
    public ImageOperationsLightCycle(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    @Override
    public void onResume(FragmentActivity activity) {
        //Ensures that ImageLoader will be resumed if the preceding activity was killed during scrolling
        imageOperations.resume();
    }
}
