package com.soundcloud.android.ads;

import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.ads.AdsOperations.AdRequestData;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.stream.StreamAdapter;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class StreamAdsController extends RecyclerView.OnScrollListener {

    private final static int EMPTY_ADS_RESPONSE_BACKOFF_MS = 1000 * 60;

    private final AdsOperations adsOperations;
    private final InlayAdOperations inlayAdOperations;
    private final InlayAdHelperFactory inlayAdHelperFactory;
    private final FeatureFlags featureFlags;
    private final FeatureOperations featureOperations;
    private final CurrentDateProvider dateProvider;
    private final EventBus eventBus;

    private Subscription fetchSubscription = RxUtils.invalidSubscription();
    private Subscription impressionsSubscription = RxUtils.invalidSubscription();

    private List<AdData> inlayAds = Collections.emptyList();
    private Optional<Long> lastEmptyResponseTime = Optional.absent();
    private Optional<InlayAdHelper> inlayAdHelper = Optional.absent();

    private String lastRequestId = Strings.EMPTY;
    private boolean wasScrollingUp;

    @Inject
    public StreamAdsController(AdsOperations adsOperations,
                               InlayAdOperations inlayAdOperations,
                               InlayAdHelperFactory inlayAdHelperFactory,
                               FeatureFlags featureFlags,
                               FeatureOperations featureOperations,
                               CurrentDateProvider dateProvider,
                               EventBus eventBus) {
        this.adsOperations = adsOperations;
        this.inlayAdOperations = inlayAdOperations;
        this.inlayAdHelperFactory = inlayAdHelperFactory;
        this.featureFlags = featureFlags;
        this.featureOperations = featureOperations;
        this.dateProvider = dateProvider;
        this.eventBus = eventBus;
    }

    public void onViewCreated(RecyclerView recyclerView, StreamAdapter adapter) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        final StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
        this.inlayAdHelper = Optional.of(inlayAdHelperFactory.create(staggeredGridLayoutManager, adapter));
    }

    public void onDestroyView() {
        fetchSubscription.unsubscribe();
        impressionsSubscription.unsubscribe();
        this.inlayAdHelper = Optional.absent();
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
        if (streamAdsEnabled() && inlayAdHelper.isPresent()) {
            inlayAdHelper.get().onScroll();
        }
    }

    public void insertAds() {
        if (streamAdsEnabled()) {
            clearExpiredAds();

            if (impressionsSubscription.isUnsubscribed() && inlayAdHelper.isPresent()) {
                impressionsSubscription = inlayAdOperations.trackImpressions(inlayAdHelper.get())
                                                           .subscribe(eventBus.queue(EventQueue.TRACKING));
            }

            if (inlayAds.isEmpty() && fetchSubscription.isUnsubscribed() && shouldFetchMoreAds()) {
                fetchSubscription = fetchInlays();
            } else {
                attemptToInsertAd();
            }
        }
    }

    private Subscription fetchInlays() {
        return adsOperations.kruxSegments()
                            .flatMap(
                                kruxSegments -> {
                                    final AdRequestData adRequestData = AdRequestData.forStreamAds(kruxSegments);
                                    lastRequestId = adRequestData.getRequestId();
                                    return adsOperations.inlayAds(adRequestData);
                                })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new FetchAppInstalls());
    }

    private boolean streamAdsEnabled() {
        return featureOperations.shouldRequestAds();
    }

    private void clearExpiredAds() {
        inlayAds = Lists.newArrayList(Iterables.filter(inlayAds, isAdExpired()));
    }

    private Predicate<AdData> isAdExpired() {
        return ad -> {
            if (ad instanceof ExpirableAd) {
                final ExpirableAd expirableAd = (ExpirableAd) ad;
                return expirableAd.getCreatedAt() + TimeUnit.MINUTES.toMillis(expirableAd.getExpiryInMins()) > dateProvider.getCurrentTime();
            }
            return false;
        };
    }

    private boolean shouldFetchMoreAds() {
        return !(lastEmptyResponseTime.isPresent()
                && Math.abs(dateProvider.getCurrentTime() - lastEmptyResponseTime.get()) < EMPTY_ADS_RESPONSE_BACKOFF_MS);
    }

    private void attemptToInsertAd() {
        checkState(Thread.currentThread() == Looper.getMainLooper().getThread());
        if (!inlayAds.isEmpty() && inlayAdHelper.isPresent()) {
            final AdData ad = inlayAds.get(0);
            final boolean inserted = inlayAdHelper.get().insertAd(ad, wasScrollingUp);

            if (inserted) {
                inlayAds.remove(ad);
                eventBus.publish(EventQueue.TRACKING, AdDeliveryEvent.adDelivered(Optional.absent(),
                                                                                  ad.getAdUrn(),
                                                                                  lastRequestId,
                                                                                  false, true));
            }
        }
    }

    private class FetchAppInstalls extends DefaultSubscriber<List<AdData>> {
        @Override
        public void onNext(List<AdData> ads) {
            if (ads.isEmpty()) {
                setLastEmptyResponseTime();
            } else {
                inlayAds = filterEnabledAds(ads);
                insertAds();
            }
        }

        @Override
        public void onError(Throwable e) {
            setLastEmptyResponseTime();
        }

        private List<AdData> filterEnabledAds(List<AdData> ads) {
            if (featureFlags.isEnabled(Flag.VIDEO_INLAYS)) {
                return ads;
            } else {
                final List<AdData> filteredAds = new ArrayList<>(ads.size());
                for (AdData ad : ads) {
                    if (ad instanceof AppInstallAd) {
                       filteredAds.add(ad);
                    }
                }
                return filteredAds;
            }
        }

        private void setLastEmptyResponseTime() {
            lastEmptyResponseTime = Optional.of(dateProvider.getCurrentTime());
        }
    }
}
