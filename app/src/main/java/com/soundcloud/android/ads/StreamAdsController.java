package com.soundcloud.android.ads;

import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.stream.StreamAdapter;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

@Singleton
public class StreamAdsController extends RecyclerView.OnScrollListener {

    private final static int EMPTY_ADS_RESPONSE_BACKOFF_MS = 1000 * 60;

    private final AdsOperations adsOperations;
    private final FeatureFlags featureFlags;
    private final FeatureOperations featureOperations;
    private final DateProvider dateProvider;

    private Optional<InlayAdInsertionHelper> inlayAdInsertionHelper = Optional.absent();

    private Subscription fetchSubscription = RxUtils.invalidSubscription();
    private List<AppInstallAd> inlayAds = Collections.emptyList();
    private Optional<Long> lastEmptyResponseTime = Optional.absent();

    private boolean wasScrollingUp;

    @Inject
    public StreamAdsController(AdsOperations adsOperations,
                               FeatureFlags featureFlags,
                               FeatureOperations featureOperations,
                               CurrentDateProvider dateProvider) {
        this.adsOperations = adsOperations;
        this.featureFlags = featureFlags;
        this.featureOperations = featureOperations;
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
        if (featureFlags.isEnabled(Flag.APP_INSTALLS) && featureOperations.shouldRequestAds()) {
            clearExpiredAds();
            if (inlayAds.isEmpty() && fetchSubscription.isUnsubscribed() && shouldFetchMoreAds()) {
                fetchSubscription = adsOperations.inlaysAds()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new AppInstallSubscriber());
            } else {
                attemptToInsertAd();
            }
        }
    }

    private void clearExpiredAds() {
        inlayAds = Lists.newArrayList(Iterables.filter(inlayAds, new Predicate<AppInstallAd>() {
            @Override
            public boolean apply(AppInstallAd ad) {
                return ad.getCreatedAt() + TimeUnit.MINUTES.toMillis(ad.getExpiryInMins()) > dateProvider.getCurrentTime();
            }
        }));
    }

    private boolean shouldFetchMoreAds() {
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
                insertAds();
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
