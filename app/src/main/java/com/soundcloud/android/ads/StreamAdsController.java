package com.soundcloud.android.ads;

import android.os.Looper;
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
import com.soundcloud.android.utils.CurrentDateProvider;
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
    private final InlayAdOperations inlayAdOperations;
    private final InlayAdHelper inlayAdHelper;
    private final FeatureFlags featureFlags;
    private final FeatureOperations featureOperations;
    private final CurrentDateProvider dateProvider;
    private final EventBus eventBus;

    private Func1<Optional<String>, Observable<List<AppInstallAd>>> fetchInlays = new Func1<Optional<String>, Observable<List<AppInstallAd>>>() {
        @Override
        public Observable<List<AppInstallAd>> call(Optional<String> kruxSegments) {
            final AdRequestData adRequestData = AdRequestData.forStreamAds(kruxSegments);
            lastRequestId = adRequestData.getRequestId();
            return adsOperations.inlaysAds(adRequestData);
        }
    };

    private Subscription fetchSubscription = RxUtils.invalidSubscription();
    private Subscription impressionsSubscriptions = RxUtils.invalidSubscription();
    private List<AppInstallAd> inlayAds = Collections.emptyList();
    private Optional<Long> lastEmptyResponseTime = Optional.absent();
    private String lastRequestId = Strings.EMPTY;

    private boolean wasScrollingUp;

    @Inject
    public StreamAdsController(AdsOperations adsOperations,
                               InlayAdOperations inlayAdOperations,
                               InlayAdHelper inlayAdHelper,
                               FeatureFlags featureFlags,
                               FeatureOperations featureOperations,
                               CurrentDateProvider dateProvider,
                               EventBus eventBus) {
        this.adsOperations = adsOperations;
        this.inlayAdOperations = inlayAdOperations;
        this.inlayAdHelper = inlayAdHelper;
        this.featureFlags = featureFlags;
        this.featureOperations = featureOperations;
        this.dateProvider = dateProvider;
        this.eventBus = eventBus;
    }

    public void clear() {
        fetchSubscription.unsubscribe();
        impressionsSubscriptions.unsubscribe();
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (newState == RecyclerView.SCROLL_STATE_SETTLING && layoutManager instanceof StaggeredGridLayoutManager) {
            insertAds((StaggeredGridLayoutManager) layoutManager);
        }
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (featureFlags.isEnabled(Flag.APP_INSTALLS)
                && featureOperations.shouldRequestAds()
                && layoutManager instanceof StaggeredGridLayoutManager) {
            inlayAdHelper.onScroll((StaggeredGridLayoutManager) layoutManager);
        }

        wasScrollingUp = dy < 0;
    }

    public void insertAds() {
        insertAds(Optional.<StaggeredGridLayoutManager>absent());
    }

    public void insertAds(StaggeredGridLayoutManager layoutManager) {
        insertAds(Optional.of(layoutManager));
    }

    protected void insertAds(Optional<StaggeredGridLayoutManager> layoutManager) {
        if (featureFlags.isEnabled(Flag.APP_INSTALLS) && featureOperations.shouldRequestAds()) {
            clearExpiredAds();

            if (impressionsSubscriptions.isUnsubscribed() && layoutManager.isPresent()) {
                impressionsSubscriptions = inlayAdOperations.trackImpressions(layoutManager.get())
                                                            .subscribe(eventBus.queue(EventQueue.TRACKING));
            }

            if (inlayAds.isEmpty() && fetchSubscription.isUnsubscribed() && shouldFetchMoreAds()) {
                fetchSubscription = adsOperations.kruxSegments()
                                                 .flatMap(fetchInlays)
                                                 .observeOn(AndroidSchedulers.mainThread())
                                                 .subscribe(new FetchAppInstalls(layoutManager));
            } else if (layoutManager.isPresent()) {
                attemptToInsertAd(layoutManager.get());
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

    private void attemptToInsertAd(StaggeredGridLayoutManager layoutManager) {
        checkState(Thread.currentThread() == Looper.getMainLooper().getThread());
        if (!inlayAds.isEmpty()) {
            final AppInstallAd ad = inlayAds.get(0);
            final boolean inserted = inlayAdHelper.insertAd(layoutManager, ad, wasScrollingUp);

            if (inserted) {
                inlayAds.remove(ad);
                eventBus.publish(EventQueue.TRACKING, AdDeliveryEvent.adDelivered(Optional.<Urn>absent(),
                                                                                  ad.getAdUrn(),
                                                                                  lastRequestId,
                                                                                  false, true));
            }
        }
    }

    private class FetchAppInstalls extends DefaultSubscriber<List<AppInstallAd>> {
        final Optional<StaggeredGridLayoutManager> layoutManager;

        private FetchAppInstalls(Optional<StaggeredGridLayoutManager> layoutManager) {
            this.layoutManager = layoutManager;
        }

        @Override
        public void onNext(List<AppInstallAd> ads) {
            if (ads.isEmpty()) {
                setLastEmptyResponseTime();
            } else {
                inlayAds = ads;

                if (layoutManager.isPresent()) {
                    insertAds(layoutManager);
                }
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
