package com.soundcloud.android.ads;

import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.stream.StreamAdapter;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.optional.Optional;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

@Singleton
public class StreamAdsController extends RecyclerView.OnScrollListener {

    private final static int EMPTY_ADS_RESPONSE_BACKOFF_MS = 1000 * 60;

    private final AdsOperations adsOperations;
    private final FeatureFlags featureFlags;
    private final DateProvider dateProvider;

    private Optional<InlayAdInsertionHelper> inlayAdInsertionHelper = Optional.absent();

    private Subscription fetchSubscription = RxUtils.invalidSubscription();
    private List<AppInstallAd> inlayAds = Collections.emptyList();
    private Optional<Long> lastEmptyResponseTime = Optional.absent();

    private boolean wasScrollingUp;

    @Inject
    public StreamAdsController(AdsOperations adsOperations, FeatureFlags featureFlags, CurrentDateProvider dateProvider) {
        this.adsOperations = adsOperations;
        this.featureFlags = featureFlags;
        this.dateProvider = dateProvider;
    }

    public void set(StaggeredGridLayoutManager layoutManager, StreamAdapter adapter) {
        this.set(new InlayAdInsertionHelper(layoutManager, adapter));
    }

    @VisibleForTesting
    void set(InlayAdInsertionHelper helper) {
        this.inlayAdInsertionHelper = Optional.of(helper);
    }

    public void clear() {
        fetchSubscription.unsubscribe();
        this.inlayAdInsertionHelper = Optional.absent();
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
            insertAds();
        }
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        wasScrollingUp = dy < 0;
    }

    public void insertAds() {
        if (featureFlags.isEnabled(Flag.APP_INSTALLS)) {
            if (inlayAds.isEmpty() && fetchSubscription.isUnsubscribed() && shouldRequestAds()) {
                fetchSubscription = adsOperations.inlaysAds()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new AppInstallSubscriber());
            } else {
                attemptToInsertAd();
            }
        }
    }

    private boolean shouldRequestAds() {
        return !(lastEmptyResponseTime.isPresent()
                && Math.abs(dateProvider.getCurrentTime() - lastEmptyResponseTime.get()) < EMPTY_ADS_RESPONSE_BACKOFF_MS);
    }

    private void attemptToInsertAd() {
        if (!inlayAds.isEmpty() && inlayAdInsertionHelper.isPresent()) {
            final boolean inserted = inlayAdInsertionHelper.get().insertAd(inlayAds.get(0), wasScrollingUp);
            if (inserted) {
                inlayAds.remove(0);
            }
        }
    }

    private class AppInstallSubscriber extends DefaultSubscriber<List<AppInstallAd>> {
        @Override
        public void onNext(List<AppInstallAd> ads) {
            if (ads.isEmpty()) {
                setLastEmptyResponseTime();
            } else {
                inlayAds = ads;
                attemptToInsertAd();
            }
        }

        @Override
        public void onError(Throwable e) {
            setLastEmptyResponseTime();
        }

        private void setLastEmptyResponseTime() {
            lastEmptyResponseTime = Optional.of(dateProvider.getCurrentTime());
        }
    }
}
