package com.soundcloud.android.image;

import com.soundcloud.android.lightcycle.DefaultLightCycleActivity;

import android.support.v7.app.ActionBarActivity;

import javax.inject.Inject;

public class ImageOperationsLightCycle extends DefaultLightCycleActivity<ActionBarActivity> {
    private final ImageOperations imageOperations;

    @Inject
    public ImageOperationsLightCycle(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    @Override
    public void onResume(ActionBarActivity activity) {
        //Ensures that ImageLoader will be resumed if the preceding activity was killed during scrolling
        imageOperations.resume();
    }
}
