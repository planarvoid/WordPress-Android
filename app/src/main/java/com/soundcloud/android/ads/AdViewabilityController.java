package com.soundcloud.android.ads;

import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;

import com.soundcloud.android.model.Urn;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AdViewabilityController extends DefaultActivityLightCycle<AppCompatActivity> {

    private final MoatViewabilityController moatViewabilityController;

    @Inject
    public AdViewabilityController(MoatViewabilityController moatViewabilityController) {
        this.moatViewabilityController = moatViewabilityController;
    }

    public void setupVideoTracking(Urn adUrn, long duration, String uuid, String monetizationType) {
        moatViewabilityController.createTrackerForAd(adUrn, duration, uuid, monetizationType);
    }

    public void updateView(String uuid, TextureView textureView) {
        moatViewabilityController.dispatchVideoViewUpdate(uuid, textureView);
    }

    public void onResume(VideoAd ad, long currentPosition)  {
        moatViewabilityController.onVideoResume(ad.getUuid(), currentPosition);
    }

    public void onPaused(VideoAd ad, long currentPosition)  {
        moatViewabilityController.onVideoPause(ad.getUuid(), currentPosition);
    }

    public void onProgressQuartileEvent(VideoAd ad, PlayableAdData.ReportingEvent event, long currentPosition) {
        final String uuid = ad.getUuid();
        switch (event) {
            case START:
                moatViewabilityController.onVideoStart(uuid, currentPosition);
                break;
            case FIRST_QUARTILE:
                moatViewabilityController.onVideoFirstQuartile(uuid, currentPosition);
                break;
            case SECOND_QUARTILE:
                moatViewabilityController.onVideoSecondQuartile(uuid, currentPosition);
                break;
            case THIRD_QUARTILE:
                moatViewabilityController.onVideoThirdQuartile(uuid, currentPosition);
                break;
            case FINISH:
                moatViewabilityController.onVideoCompletion(uuid, currentPosition);
                moatViewabilityController.stopVideoTracking(uuid);
                break;
            default:
                // no-op
                break;
        }
    }

    public void stopVideoTracking(String uuid) {
        moatViewabilityController.stopVideoTracking(uuid);
    }

    void startOverlayTracking(View imageView, OverlayAdData overlayAdData) {
        moatViewabilityController.startOverlayTracking(imageView, overlayAdData);
    }

    void stopOverlayTracking() {
        moatViewabilityController.stopOverlayTracking();
    }
}
