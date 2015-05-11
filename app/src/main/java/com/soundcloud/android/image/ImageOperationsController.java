package com.soundcloud.android.image;

import com.soundcloud.lightcycle.DefaultLightCycleActivity;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class ImageOperationsController extends DefaultLightCycleActivity<AppCompatActivity> {
    private final ImageOperations imageOperations;

    @Inject
    public ImageOperationsController(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        //Ensures that ImageLoader will be resumed if the preceding activity was killed during scrolling
        imageOperations.resume();
    }
}
