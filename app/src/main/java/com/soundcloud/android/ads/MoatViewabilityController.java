package com.soundcloud.android.ads;

import com.moat.analytics.mobile.scl.MoatAdEvent;
import com.moat.analytics.mobile.scl.MoatAdEventType;
import com.moat.analytics.mobile.scl.MoatFactory;
import com.moat.analytics.mobile.scl.NativeDisplayTracker;
import com.moat.analytics.mobile.scl.ReactiveVideoTracker;
import com.moat.analytics.mobile.scl.ReactiveVideoTrackerPlugin;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.VideoSurfaceProvider;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.view.TextureView;
import android.view.View;

import javax.inject.Inject;
import java.util.HashMap;

public class MoatViewabilityController {

    private final DeviceHelper deviceHelper;
    private final VideoSurfaceProvider videoSurfaceProvider;
    private final ReactiveVideoTrackerPlugin videoPlugin;

    private Optional<MoatFactory> moatFactory = Optional.absent();
    private Optional<NativeDisplayTracker> displayTracker = Optional.absent();
    private HashMap<String, ReactiveVideoTracker> videoTrackers = new HashMap<>(2); // Stream and player can have video ads

    @Inject
    MoatViewabilityController(Context context,
                              DeviceHelper deviceHelper,
                              VideoSurfaceProvider videoSurfaceProvider) {
        this.deviceHelper = deviceHelper;
        this.videoSurfaceProvider = videoSurfaceProvider;
        videoPlugin = new ReactiveVideoTrackerPlugin(context.getString(R.string.moat_video_partner_id));
    }

    void createTrackerForAd(Urn adUrn, long duration, String uuid, String monetizationType) {
        final Optional<TextureView> videoView = videoSurfaceProvider.getTextureView(uuid);
        videoView.ifPresent(view -> {
            ReactiveVideoTracker tracker = getMoatFactory().get().createCustomTracker(videoPlugin);
            tracker.trackVideoAd(getMoatSlicers(adUrn, monetizationType), Long.valueOf(duration).intValue(), view);
            videoTrackers.put(uuid, tracker);
        });
    }

    void onVideoStart(String uuid, long currentPosition) {
        dispatchVideoEvent(uuid, MoatAdEventType.AD_EVT_START, currentPosition);
    }

    void onVideoFirstQuartile(String uuid, long currentPosition) {
        dispatchVideoEvent(uuid, MoatAdEventType.AD_EVT_FIRST_QUARTILE, currentPosition);
    }

    void onVideoSecondQuartile(String uuid, long currentPosition) {
        dispatchVideoEvent(uuid, MoatAdEventType.AD_EVT_MID_POINT, currentPosition);
    }

    void onVideoThirdQuartile(String uuid, long currentPosition) {
        dispatchVideoEvent(uuid, MoatAdEventType.AD_EVT_THIRD_QUARTILE, currentPosition);
    }

    void onVideoCompletion(String uuid, long currentPosition) {
        dispatchVideoEvent(uuid, MoatAdEventType.AD_EVT_COMPLETE, currentPosition);
    }

    void onVideoPause(String uuid, long currentPosition) {
        dispatchVideoEvent(uuid, MoatAdEventType.AD_EVT_PAUSED, currentPosition);
    }

    void onVideoResume(String uuid, long currentPosition) {
        dispatchVideoEvent(uuid, MoatAdEventType.AD_EVT_PLAYING, currentPosition);
    }

    void dispatchVideoViewUpdate(String uuid, TextureView textureView) {
        trackerForAd(uuid).ifPresent(tracker -> tracker.changeTargetView(textureView));
    }

    private void dispatchVideoEvent(String uuid, MoatAdEventType eventType, long position) {
        trackerForAd(uuid).ifPresent(tracker -> tracker.dispatchEvent(new MoatAdEvent(eventType, Long.valueOf(position).intValue())));
    }

    void stopVideoTracking(String uuid) {
        trackerForAd(uuid).ifPresent(tracker -> {
            tracker.stopTracking();
            videoTrackers.remove(uuid);
        });
    }

    private Optional<ReactiveVideoTracker> trackerForAd(String videoUuid) {
        if (videoTrackers.containsKey(videoUuid)) {
            return Optional.of(videoTrackers.get(videoUuid));
        }
        return Optional.absent();
    }

    void startOverlayTracking(View imageView, OverlayAdData adData) {
        if (adData instanceof InterstitialAd) {
            final HashMap<String, String> moatSlicers = getMoatSlicers(adData.getAdUrn(), adData.getMonetizationType().key());
            final NativeDisplayTracker tracker = getMoatFactory().get().createNativeDisplayTracker(imageView, moatSlicers);
            tracker.startTracking();
            displayTracker = Optional.of(tracker);
        }
    }

    void stopOverlayTracking() {
        displayTracker.ifPresent(tracker -> {
            tracker.stopTracking();
            displayTracker = Optional.absent();
        });
    }

    private Optional<MoatFactory> getMoatFactory() {
        if (!moatFactory.isPresent()) {
            moatFactory = Optional.of(MoatFactory.create());
        }
        return moatFactory;
    }

    private HashMap<String, String> getMoatSlicers(Urn adUrn, String monetizationType) {
        final String[] urnComponents = adUrn.getStringId().split("-");
        final String appVersion = "android-" + String.valueOf(deviceHelper.getAppVersionCode());
        if (urnComponents.length == 2) {
            final String slicerTwo = slicerForMonetizationType(monetizationType);
            if (monetizationType.equals(AdData.MonetizationType.INTERSTITIAL.key())) {
                return slicersForInterstitial(urnComponents[0], urnComponents[1], appVersion, slicerTwo);
            } else {
                return slicersForVideo(urnComponents[0], urnComponents[1], appVersion, slicerTwo);
            }
        }
        return new HashMap<>(4);
    }

    private HashMap<String, String> slicersForVideo(String levelOne, String levelTwo, String slicerOne, String slicerTwo) {
        final HashMap<String, String> slicers = new HashMap<>(4);
        slicers.put("level1", levelOne);
        slicers.put("level2", levelTwo);
        slicers.put("slicer1", slicerOne);
        slicers.put("slicer2", slicerTwo);
        return slicers;
    }

    private HashMap<String, String> slicersForInterstitial(String levelOne, String levelTwo, String slicerOne, String slicerTwo) {
        final HashMap<String, String> slicers = new HashMap<>(4);
        slicers.put("moatClientLevel1", levelOne);
        slicers.put("moatClientLevel2", levelTwo);
        slicers.put("moatClientSlicer1", slicerOne);
        slicers.put("moatClientSlicer2", slicerTwo);
        return slicers;
    }

    private String slicerForMonetizationType(String monetizationType) {
        if (monetizationType.equals(AdData.MonetizationType.VIDEO.key())) {
            return "video";
        } else if (monetizationType.equals(AdData.MonetizationType.INLAY.key())) {
            return "video-inlay";
        } else {
            return "interstitial";
        }
    }
}
