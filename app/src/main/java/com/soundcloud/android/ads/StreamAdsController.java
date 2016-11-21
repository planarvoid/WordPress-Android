package com.soundcloud.android.ads;

import android.os.Looper;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import com.soundcloud.android.ads.AdsOperations.AdRequestData;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
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
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import static com.soundcloud.java.checks.Preconditions.checkState;

@Singleton
public class StreamAdsController extends RecyclerView.OnScrollListener {

    private final static int EMPTY_ADS_RESPONSE_BACKOFF_MS = 1000 * 60;

    private final AdsOperations adsOperations;
    private final FeatureFlags featureFlags;
    private final FeatureOperations featureOperations;
    private final DateProvider dateProvider;
    private final EventBus eventBus;

    private Func1<Optional<String>, Observable<List<AppInstallAd>>> fetchInlays = new Func1<Optional<String>, Observable<List<AppInstallAd>>>() {
        @Override
        public Observable<List<AppInstallAd>> call(Optional<String> kruxSegments) {
            final AdRequestData adRequestData = AdRequestData.forStreamAds(kruxSegments);
            lastRequestId = adRequestData.getRequestId();
            return adsOperations.inlaysAds(adRequestData);
        }
    };

    private Optional<InlayAdInsertionHelper> inlayAdInsertionHelper = Optional.absent();

    private Subscription fetchSubscription = RxUtils.invalidSubscription();
    private List<AppInstallAd> inlayAds = Collections.emptyList();
    private Optional<Long> lastEmptyResponseTime = Optional.absent();
    private String lastRequestId = Strings.EMPTY;

    private boolean wasScrollingUp;

    @Inject
    public StreamAdsController(AdsOperations adsOperations,
                               FeatureFlags featureFlags,
                               FeatureOperations featureOperations,
                               CurrentDateProvider dateProvider,
                               EventBus eventBus) {
        this.adsOperations = adsOperations;
        this.featureFlags = featureFlags;
        this.featureOperations = featureOperations;
        this.dateProvider = dateProvider;
        this.eventBus = eventBus;
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
                fetchSubscription = adsOperations.kruxSegments()
                                                 .flatMap(fetchInlays)
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
        checkState(Thread.currentThread() == Looper.getMainLooper().getThread());
        if (!inlayAds.isEmpty() && inlayAdInsertionHelper.isPresent()) {
            final AppInstallAd ad = inlayAds.get(0);
            final boolean inserted = inlayAdInsertionHelper.get().insertAd(ad, wasScrollingUp);
            if (inserted) {
                inlayAds.remove(ad);
                eventBus.publish(EventQueue.TRACKING, AdDeliveryEvent.adDelivered(Optional.<Urn>absent(),
                                                                                  ad.getAdUrn(),
                                                                                  lastRequestId,
                                                                                  false, true));
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
