package com.soundcloud.android.ads;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class AdIdHelper {

    private static final String TAG = AdIdHelper.class.getSimpleName();

    private final AdIdWrapper adIdWrapper;
    private final Scheduler scheduler;

    private volatile String adId;
    private volatile boolean adIdTracking;

    @Inject
    public AdIdHelper(AdIdWrapper adIdWrapper, @Named("API") Scheduler scheduler) {
        this.adIdWrapper = adIdWrapper;
        this.scheduler = scheduler;
    }

    public void init() {
        if (adIdWrapper.isPlayServicesAvailable()) {
            getAdInfo()
                   .subscribeOn(scheduler)
                   .observeOn(AndroidSchedulers.mainThread())
                   .subscribe(new AdInfoSubscriber());
        }
    }

    public boolean isAvailable() {
        return adId != null;
    }

    @Nullable
    public String getAdId() {
        return adId;
    }

    public boolean getAdIdTracking() {
        return adIdTracking;
    }

    private Observable<AdvertisingIdClient.Info> getAdInfo() {
        return Observable.create(new Observable.OnSubscribe<AdvertisingIdClient.Info>() {
            @Override
            public void call(Subscriber<? super AdvertisingIdClient.Info> subscriber) {
                try {
                    subscriber.onNext(adIdWrapper.getAdInfo());
                    subscriber.onCompleted();
                } catch (IOException | GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    private class AdInfoSubscriber extends DefaultSubscriber<AdvertisingIdClient.Info> {
        @Override
        public void onNext(AdvertisingIdClient.Info adInfo) {
            adId = adInfo.getId();
            adIdTracking = !adInfo.isLimitAdTrackingEnabled(); // We reverse this value to match the iOS param
            Log.i(TAG, "Loaded ADID: " + adId + "\nTracking:" + adIdTracking);
        }

        @Override
        public void onError(Throwable e) {
            Log.e(TAG, "Failed to load ADID", e);
        }
    }

}
