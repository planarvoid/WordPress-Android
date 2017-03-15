package com.soundcloud.android.ads;

import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AdViewabilityController extends DefaultActivityLightCycle<AppCompatActivity> {

    private final MoatViewabilityController moatViewabilityController;

    private final boolean shouldTrackWithMoat;

    @Inject
    public AdViewabilityController(ApplicationProperties applicationProperties,
                                   MoatViewabilityController moatViewabilityController) {
        this.moatViewabilityController = moatViewabilityController;
        shouldTrackWithMoat = applicationProperties.canUseMoatForAdViewability();
    }

    public void setupVideoTracking(Urn adUrn, long duration, String uuid, String monetizationType) {
        if (shouldTrackWithMoat) {
            moatViewabilityController.createTrackerForAd(adUrn, duration, uuid, monetizationType);
        }
    }

    public void updateView(String uuid, TextureView textureView) {
        if (shouldTrackWithMoat) {
            moatViewabilityController.dispatchVideoViewUpdate(uuid, textureView);
        }
    }

    public void onResume(VideoAd ad, long currentPosition)  {
        if (shouldTrackWithMoat) {
            moatViewabilityController.onVideoResume(ad.getUuid(), currentPosition);
        }
    }

    public void onPaused(VideoAd ad, long currentPosition)  {
        if (shouldTrackWithMoat) {
            moatViewabilityController.onVideoPause(ad.getUuid(), currentPosition);
        }
    }

    public void onProgressQuartileEvent(VideoAd ad, PlayableAdData.ReportingEvent event, long currentPosition) {
        final String uuid = ad.getUuid();
        if (shouldTrackWithMoat) {
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
    }

    public void stopVideoTracking(String uuid) {
        if (shouldTrackWithMoat) {
            moatViewabilityController.stopVideoTracking(uuid);
        }
    }

    void startOverlayTracking(View imageView, OverlayAdData overlayAdData) {
        if (shouldTrackWithMoat) {
            moatViewabilityController.startOverlayTracking(imageView, overlayAdData);
        }
    }

    void stopOverlayTracking() {
        if (shouldTrackWithMoat) {
            moatViewabilityController.stopOverlayTracking();
        }
    }
}
