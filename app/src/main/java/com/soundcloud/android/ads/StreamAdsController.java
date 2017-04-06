package com.soundcloud.android.ads;

import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.ads.AdsOperations.AdRequestData;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.InlayAdEvent;
import com.soundcloud.android.events.PlayerUIEvent;
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
import dagger.Lazy;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.os.Looper;
import android.support.v4.app.Fragment;
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

    private final static long EMPTY_ADS_RESPONSE_BACKOFF = TimeUnit.SECONDS.toMillis(60);

    private final AdsOperations adsOperations;
    private final AdViewabilityController adViewabilityController;
    private final InlayAdOperations inlayAdOperations;
    private final InlayAdHelperFactory inlayAdHelperFactory;
    private final InlayAdStateProvider stateProvider;
    private final Lazy<InlayAdPlayer> inlayAdPlayer;
    private final FeatureOperations featureOperations;
    private final CurrentDateProvider dateProvider;
    private final EventBus eventBus;

    private final CompositeSubscription subscriptions = new CompositeSubscription();

    private Subscription fetchSubscription = RxUtils.invalidSubscription();

    private List<AdData> availableAds = Collections.emptyList();
    private List<AdData> insertedAds = new ArrayList<>(AdConstants.MAX_INLAYS_ON_SCREEN);

    private Optional<Long> lastEmptyResponseTime = Optional.absent();
    private Optional<InlayAdHelper> inlayAdHelper = Optional.absent();

    private String lastRequestId = Strings.EMPTY;

    private boolean wasScrollingUp;
    private boolean streamAdsEnabled;
    private boolean isInFullscreen;

    @Inject
    public StreamAdsController(AdsOperations adsOperations,
                               AdViewabilityController adViewabilityController,
                               InlayAdOperations inlayAdOperations,
                               InlayAdHelperFactory inlayAdHelperFactory,
                               InlayAdStateProvider inlayAdStateProvider,
                               Lazy<InlayAdPlayer> inlayAdPlayer,
                               FeatureOperations featureOperations,
                               CurrentDateProvider dateProvider,
                               EventBus eventBus) {
        this.adsOperations = adsOperations;
        this.adViewabilityController = adViewabilityController;
        this.inlayAdOperations = inlayAdOperations;
        this.inlayAdHelperFactory = inlayAdHelperFactory;
        this.stateProvider = inlayAdStateProvider;
        this.inlayAdPlayer = inlayAdPlayer;
        this.featureOperations = featureOperations;
        this.dateProvider = dateProvider;
        this.eventBus = eventBus;
    }

    public void onViewCreated(RecyclerView recyclerView, StreamAdapter adapter) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        final StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
        this.inlayAdHelper = Optional.of(inlayAdHelperFactory.create(staggeredGridLayoutManager, adapter));

        subscriptions.addAll(featureOperations.adsEnabled()
                                              .startWith(Boolean.TRUE)
                                              .subscribe(new AdsEnabled()),
                             inlayAdOperations.subscribe(inlayAdHelper.get()),
                             inlayAdHelper.get().subscribe(),
                             eventBus.queue(EventQueue.PLAYER_UI)
                                     .subscribe(new PlayerUISubscriber()));
    }

    public void onFocusGain()  {
        if (inlayAdHelper.isPresent() && !isInFullscreen()) {
            inlayAdHelper.get().onChangeToAdsOnScreen(true);
        }
    }

    public void onFocusLoss(boolean wasDueToTabChange) {
        if (inlayAdHelper.isPresent() && !isInFullscreen() && wasDueToTabChange) {
            publishNoVideoOnScreenEvent();
        }
    }

    public void onResume(boolean hasFocus) {
        if (hasFocus) {
            onFocusGain();
        } else {
            onFocusLoss(false);
        }
    }

    public void onPause(Fragment fragment) {
        if (!isOrientationChanging(fragment) && !isInFullscreen()) {
            publishNoVideoOnScreenEvent();
        }
    }

    private boolean isOrientationChanging(Fragment fragment) {
        return fragment.getActivity().isChangingConfigurations();
    }

    public void onDestroyView() {
        fetchInlays().unsubscribe();
        subscriptions.clear();
        this.inlayAdHelper = Optional.absent();
    }

    public void onDestroy() {
        if (streamAdsEnabled) {
            reset();
        }
    }

    private void reset() {
        for (AdData ad : insertedAds) {
            if (ad instanceof VideoAd) {
                final String uuid = ((VideoAd) ad).getUuid();
                stateProvider.remove(uuid);
                adViewabilityController.stopVideoTracking(uuid);
            }
        }
        isInFullscreen = false;
        insertedAds.clear();
        inlayAdPlayer.get().reset();
    }

    private void publishNoVideoOnScreenEvent() {
        eventBus.publish(EventQueue.INLAY_AD, InlayAdEvent.NoVideoOnScreen.create(dateProvider.getCurrentDate(), false));
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
        if (streamAdsEnabled && inlayAdHelper.isPresent()) {
            inlayAdHelper.get().onChangeToAdsOnScreen(false);
        }
    }

    public void insertAds() {
        if (streamAdsEnabled) {
            clearExpiredAds();

            if (availableAds.isEmpty() && fetchSubscription.isUnsubscribed() && shouldFetchMoreAds()) {
                fetchSubscription = fetchInlays();
            } else {
                attemptToInsertAd();
            }
        }
    }

    public void setFullscreenEnabled() {
        this.isInFullscreen = true;
    }

    void setFullscreenDisabled() {
        this.isInFullscreen = false;
    }

    public boolean isInFullscreen() {
        return isInFullscreen;
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

    private void clearExpiredAds() {
        availableAds = Lists.newArrayList(Iterables.filter(availableAds, isAdExpired()));
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
                && Math.abs(dateProvider.getCurrentTime() - lastEmptyResponseTime.get()) < EMPTY_ADS_RESPONSE_BACKOFF);
    }

    private void attemptToInsertAd() {
        checkState(Thread.currentThread() == Looper.getMainLooper().getThread());
        if (!availableAds.isEmpty() && inlayAdHelper.isPresent()) {
            final AdData ad = availableAds.get(0);
            final boolean inserted = inlayAdHelper.get().insertAd(ad, wasScrollingUp);

            if (inserted) {
                insertedAds.add(ad);
                availableAds.remove(ad);
                eventBus.publish(EventQueue.TRACKING, AdDeliveryEvent.adDelivered(Optional.absent(),
                                                                                  ad.getAdUrn(),
                                                                                  lastRequestId,
                                                                                  false, true));
            }
        }
    }

    private class AdsEnabled extends DefaultSubscriber<Boolean> {
        @Override
        public void onNext(Boolean ignored) {
            streamAdsEnabled = featureOperations.shouldRequestAds();
        }
    }

    private class FetchAppInstalls extends DefaultSubscriber<List<AdData>> {
        @Override
        public void onNext(List<AdData> ads) {
            if (ads.isEmpty()) {
                setLastEmptyResponseTime();
            } else {
                availableAds = ads;
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

    private class PlayerUISubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            if (event.getKind() == PlayerUIEvent.PLAYER_EXPANDED) {
                publishNoVideoOnScreenEvent();
            }
        }
    }
}
