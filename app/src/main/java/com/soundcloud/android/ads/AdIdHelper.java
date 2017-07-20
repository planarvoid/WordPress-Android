package com.soundcloud.android.ads;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.rx.observers.LambdaObserver;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

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
    public AdIdHelper(AdIdWrapper adIdWrapper,
                      @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.adIdWrapper = adIdWrapper;
        this.scheduler = scheduler;
    }

    public void init() {
        if (adIdWrapper.isPlayServicesAvailable()) {
            Observable.fromCallable(adIdWrapper::getAdInfo)
                      .subscribeOn(scheduler)
                      .observeOn(AndroidSchedulers.mainThread())
                      .subscribeWith(LambdaObserver.onNext(adInfo -> {
                          if (!adInfo.isPresent()) {
                              adId = Optional.absent();
                              adIdTracking = false;
                          } else if (adInfo.get().getId() == null) {
                              final String errorMessage = "Got adInfo(" + adInfo.get() + ") with null adInfo.getId";
                              ErrorUtils.handleSilentException(errorMessage, new MissingAdInfoIdException(errorMessage));
                              adId = Optional.absent();
                              adIdTracking = false;
                          } else {
                              adId = Optional.of(adInfo.get().getId());
                              adIdTracking = !adInfo.get().isLimitAdTrackingEnabled(); // We reverse this value to match the iOS param
                          }

                          Log.i(TAG, "Loaded ADID: " + adId + "\nTracking:" + adIdTracking);
                      }));
        }
    }

    public Optional<String> getAdId() {
        return adId;
    }

    public boolean getAdIdTracking() {
        return adIdTracking;
    }

    private static final class MissingAdInfoIdException extends Exception {
        MissingAdInfoIdException(String message) {
            super(message);
        }
    }
}
