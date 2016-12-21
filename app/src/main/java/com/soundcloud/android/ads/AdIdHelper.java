package com.soundcloud.android.ads;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class AdIdHelper {

    private static final String TAG = AdIdHelper.class.getSimpleName();

    private final AdIdWrapper adIdWrapper;
    private final Scheduler scheduler;

    private volatile Optional<String> adId = Optional.absent();
    private volatile boolean adIdTracking;

    @Inject
    public AdIdHelper(AdIdWrapper adIdWrapper, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.adIdWrapper = adIdWrapper;
        this.scheduler = scheduler;
    }

    public void init() {
        if (adIdWrapper.isPlayServicesAvailable()) {

            Observable
                    .fromCallable(adIdWrapper::getAdInfo)
                    .subscribeOn(scheduler)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new AdInfoSubscriber());
        }
    }

    public Optional<String> getAdId() {
        return adId;
    }

    public boolean getAdIdTracking() {
        return adIdTracking;
    }

    private class AdInfoSubscriber extends DefaultSubscriber<AdvertisingIdClient.Info> {
        @Override
        public void onNext(@Nullable AdvertisingIdClient.Info adInfo) {
            if (adInfo == null) {
                adId = Optional.absent();
                adIdTracking = false;
            } else {
                adId = Optional.of(adInfo.getId());
                adIdTracking = !adInfo.isLimitAdTrackingEnabled(); // We reverse this value to match the iOS param
            }

            Log.i(TAG, "Loaded ADID: " + adId + "\nTracking:" + adIdTracking);
        }
    }

}
