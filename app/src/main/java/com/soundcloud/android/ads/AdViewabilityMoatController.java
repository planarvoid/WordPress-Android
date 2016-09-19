package com.soundcloud.android.ads;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.moat.analytics.mobile.MoatFactory;
import com.moat.analytics.mobile.NativeDisplayTracker;
import com.soundcloud.android.R;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
public class AdViewabilityMoatController extends DefaultActivityLightCycle<AppCompatActivity> {

    private final DeviceHelper deviceHelper;
    private final FeatureFlags featureFlags;
    private final Context context;

    private Optional<MoatFactory> moatFactory = Optional.absent();
    private Optional<NativeDisplayTracker> displayTracker = Optional.absent();

    private WeakReference<Activity> currentActivity;

    @Inject
    public AdViewabilityMoatController(DeviceHelper deviceHelper, FeatureFlags featureFlags, Context context) {
        this.deviceHelper = deviceHelper;
        this.featureFlags = featureFlags;
        this.context = context;
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        currentActivity = new WeakReference<Activity>(activity);
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        moatFactory = Optional.absent();
    }

    public void startOverlayTracking(View imageView, OverlayAdData overlayAdData) {
        createMoatFactoryForCurrentActivity();

        if (featureFlags.isEnabled(Flag.MOAT_ADS_VIEWABILITY) && moatFactory.isPresent() && overlayAdData instanceof InterstitialAd) {
            NativeDisplayTracker tracker = moatFactory.get().createNativeDisplayTracker(imageView, context.getString(R.string.moat_display_partner_id));
            tracker.track(getMoatSlicers(overlayAdData));
            displayTracker = Optional.of(tracker);
        }
    }

    public void stopOverlayTracking() {
        if (displayTracker.isPresent()) {
            displayTracker.get().stopTracking();
            displayTracker = Optional.absent();
        }
    }

    private void createMoatFactoryForCurrentActivity() {
        if (featureFlags.isEnabled(Flag.MOAT_ADS_VIEWABILITY) && !moatFactory.isPresent()
                && currentActivity != null && currentActivity.get() != null) {
            moatFactory = Optional.of(MoatFactory.create(currentActivity.get()));
        }
    }

    private HashMap<String, String> getMoatSlicers(AdData adData) {
        final HashMap<String, String> adIds = new HashMap<>();
        final String[] urnComponents = adData.getAdUrn().getStringId().split("-");

        if (urnComponents.length == 2) {
            adIds.put("moatClientLevel1", urnComponents[0]);
            adIds.put("moatClientLevel2", urnComponents[1]);
            adIds.put("moatClientSlicer1", "android-" + String.valueOf(deviceHelper.getAppVersionCode()));
            adIds.put("moatClientSlicer2", "interstitial");
        }

        return adIds;
    }

}
