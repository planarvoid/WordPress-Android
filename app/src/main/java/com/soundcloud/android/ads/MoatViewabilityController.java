package com.soundcloud.android.ads;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.view.TextureView;
import android.view.View;

import com.moat.analytics.mobile.scl.MoatAdEvent;
import com.moat.analytics.mobile.scl.MoatAdEventType;
import com.moat.analytics.mobile.scl.MoatFactory;
import com.moat.analytics.mobile.scl.NativeDisplayTracker;
import com.moat.analytics.mobile.scl.NativeVideoTracker;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.VideoSurfaceProvider;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.optional.Optional;

import java.util.HashMap;

import javax.inject.Inject;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
public class MoatViewabilityController {

    private final Context context;
    private final AdsOperations adsOperations;
    private final DeviceHelper deviceHelper;
    private final VideoSurfaceProvider videoSurfaceProvider;

    private Optional<MoatFactory> moatFactory = Optional.absent();
    private Optional<NativeDisplayTracker> displayTracker = Optional.absent();
    private Optional<NativeVideoTracker> videoTracker = Optional.absent();

    private Urn currentAdUrn = Urn.NOT_SET;

    @Inject
    public MoatViewabilityController(Context context,
                                     AdsOperations adsOperations,
                                     DeviceHelper deviceHelper,
                                     VideoSurfaceProvider videoSurfaceProvider) {
        this.context = context;
        this.adsOperations = adsOperations;
        this.deviceHelper = deviceHelper;
        this.videoSurfaceProvider = videoSurfaceProvider;
    }

    public void startVideoTracking(Activity currentActivity, MediaPlayer mediaPlayer, Urn urn) {
        final Optional<TextureView> videoView = videoSurfaceProvider.getTextureView(urn);
        final Optional<AdData> adData = adsOperations.getCurrentTrackAdData();
        if (videoView.isPresent() && adData.isPresent() && adData.get() instanceof VideoAd) {
            createMoatFactoryIfNeeded();

            final NativeVideoTracker tracker = moatFactory.get().createNativeVideoTracker(context.getString(R.string.moat_video_partner_id));
            tracker.setActivity(currentActivity);
            tracker.trackVideoAd(getMoatSlicers(adData.get()), mediaPlayer, videoView.get());

            currentAdUrn = urn;
            videoTracker = Optional.of(tracker);
        }
    }

    public void updateActivityIfTrackingVideo(Activity activity) {
        if (videoTracker.isPresent()) {
            videoTracker.get().setActivity(activity);
        }
    }

    public void updateViewIfTrackingVideo(Urn urn, TextureView textureView) {
        if (videoTracker.isPresent() && urn.equals(currentAdUrn)) {
            videoTracker.get().changeTargetView(textureView);
        }
    }

    public void onVideoCompletion() {
        if (videoTracker.isPresent()) {
            final MoatAdEvent completionEvent = new MoatAdEvent(MoatAdEventType.AD_EVT_COMPLETE);
            videoTracker.get().dispatchEvent(completionEvent);
        }
    }

    public void stopVideoTracking() {
        if (videoTracker.isPresent()) {
            videoTracker.get().stopTracking();

            currentAdUrn = Urn.NOT_SET;
            videoTracker = Optional.absent();
        }
    }

    public void startOverlayTracking(Activity currentActivity, View imageView, OverlayAdData overlayAdData) {
        if (overlayAdData instanceof InterstitialAd) {
            createMoatFactoryIfNeeded();

            final NativeDisplayTracker tracker = moatFactory.get().createNativeDisplayTracker(imageView,
                                                                                              context.getString(R.string.moat_display_partner_id),
                                                                                              getMoatSlicers(overlayAdData));
            tracker.setActivity(currentActivity);
            tracker.startTracking();

            displayTracker = Optional.of(tracker);
        }
    }

    public void stopOverlayTracking() {
        if (displayTracker.isPresent()) {
            displayTracker.get().stopTracking();
            displayTracker = Optional.absent();
        }
    }

    private void createMoatFactoryIfNeeded() {
        if (!moatFactory.isPresent()) {
            moatFactory = Optional.of(MoatFactory.create());
        }
    }

    private HashMap<String, String> getMoatSlicers(AdData adData) {
        final HashMap<String, String> adIds = new HashMap<>();
        final String[] urnComponents = adData.getAdUrn().getStringId().split("-");

        if (urnComponents.length == 2) {
            adIds.put("moatClientLevel1", urnComponents[0]);
            adIds.put("moatClientLevel2", urnComponents[1]);
            adIds.put("moatClientSlicer1", "android-" + String.valueOf(deviceHelper.getAppVersionCode()));
            adIds.put("moatClientSlicer2", adData instanceof VideoAd ? "video" : "interstitial");
        }

        return adIds;
    }
}
