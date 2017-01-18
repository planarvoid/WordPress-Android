package com.soundcloud.android.ads;

import android.app.Activity;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AdViewabilityController extends DefaultActivityLightCycle<AppCompatActivity> {

    private final ApplicationProperties applicationProperties;
    private final MoatViewabilityController moatViewabilityController;

    private WeakReference<Activity> currentActivity;

    @Inject
    public AdViewabilityController(ApplicationProperties applicationProperties,
                                   MoatViewabilityController moatViewabilityController) {
        this.applicationProperties = applicationProperties;
        this.moatViewabilityController = moatViewabilityController;
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        currentActivity = new WeakReference<>(activity);
        if (shouldTrackWithMoat()) {
            moatViewabilityController.updateActivityIfTrackingVideo(activity);
        }
    }

    public void startVideoTracking(MediaPlayer mediaPlayer, Urn urn) {
        if (shouldTrackWithMoat()) {
            moatViewabilityController.startVideoTracking(currentActivity.get(), mediaPlayer, urn);
        }
    }

    public void updateVideoView(Urn urn, TextureView textureView) {
        if (shouldTrackWithMoat()) {
            moatViewabilityController.updateViewIfTrackingVideo(urn, textureView);
        }
    }

    public void onVideoCompletion() {
        if (shouldTrackWithMoat()) {
            moatViewabilityController.onVideoCompletion();
            moatViewabilityController.stopVideoTracking();
        }
    }

    public void stopVideoTracking() {
        if (shouldTrackWithMoat()) {
            moatViewabilityController.stopVideoTracking();
        }
    }

    void startOverlayTracking(View imageView, OverlayAdData overlayAdData) {
        if (shouldTrackWithMoat()) {
            moatViewabilityController.startOverlayTracking(currentActivity.get(), imageView, overlayAdData);
        }
    }

    void stopOverlayTracking() {
        if (shouldTrackWithMoat()) {
            moatViewabilityController.stopOverlayTracking();
        }
    }

    private boolean shouldTrackWithMoat() {
         return applicationProperties.canUseMoatForAdViewability()
                 && currentActivity != null
                 && currentActivity.get() != null;
    }
}
