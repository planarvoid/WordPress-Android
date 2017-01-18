package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import com.soundcloud.android.ads.AdsOperations.AdRequestData;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.stream.StreamAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;

import org.mockito.Spy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verifyZeroInteractions;

public class StreamAdsControllerTest extends AndroidUnitTest {

    private static final List<AppInstallAd> appInstalls = AdFixtures.getAppInstalls();
    private static final boolean SCROLLING_DOWN = false;
    private static final boolean SCROLLING_UP = true;

    @Mock private AdsOperations adsOperations;
    @Mock private InlayAdHelperFactory inlayAdHelperFactory;
    @Mock private InlayAdOperations inlayAdOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private CurrentDateProvider dateProvider;
    @Mock private InlayAdHelper inlayAdHelper;
    @Mock private RecyclerView recycler;
    @Mock private StreamAdapter adapter;
    @Mock private StaggeredGridLayoutManager layoutManager;
    @Spy private TestEventBus eventBus = new TestEventBus();

    private StreamAdsController controller;

    @Before
    public void setUp() {
        controller = spy(new StreamAdsController(adsOperations, inlayAdOperations, inlayAdHelperFactory, featureOperations, dateProvider, eventBus));

        when(recycler.getLayoutManager()).thenReturn(layoutManager);
        when(featureOperations.shouldRequestAds()).thenReturn(true);
        when(adsOperations.kruxSegments()).thenReturn(Observable.just(Optional.absent()));
        when(inlayAdOperations.trackImpressions(inlayAdHelper)).thenReturn(Observable.empty());
        when(inlayAdHelperFactory.create(any(StaggeredGridLayoutManager.class), any(StreamAdapter.class))).thenReturn(inlayAdHelper);

        controller.onViewCreated(recycler, adapter);
    }

    @Test
    public void onScrollStateChangedInsertsAdsOnSettling() {
        controller.onScrollStateChanged(recycler, RecyclerView.SCROLL_STATE_SETTLING);

        verify(controller).insertAds();
    }

    @Test
    public void onScrollStateChangedDoesntInsertAdsOnIdleOrDragging() {
        controller.onScrollStateChanged(recycler, RecyclerView.SCROLL_STATE_IDLE);
        controller.onScrollStateChanged(recycler, RecyclerView.SCROLL_STATE_DRAGGING);

        verify(controller, never()).insertAds();
    }

    @Test
    public void onScrolledCallsInlayAdHelperOnScroll() {
        controller.onScrolled(recycler, 9000, 42);

        verify(inlayAdHelper).onScroll();
    }

    @Test
    public void onScrolledDosNotCallInlayAdHelperOnScrollWithoutFetchAdsFeature() {
        when(featureOperations.shouldRequestAds()).thenReturn(false);
        controller.onScrolled(recycler, 9000, 42);
        verifyZeroInteractions(eventBus);
    }

    @Test
    public void insertAdsDoesNotFetchInlayAdsIfUserHasShouldntFetchAdsFeature() {
        when(featureOperations.shouldRequestAds()).thenReturn(false);

        controller.insertAds();

        verify(adsOperations, never()).inlaysAds(any(AdRequestData.class));
    }

    @Test
    public void insertAdsFetchesInlayAdsIfUserHasFetchAdsFeature() {
        when(featureOperations.shouldRequestAds()).thenReturn(true);
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(justAppInstalls());

        controller.insertAds();

        verify(adsOperations).inlaysAds(any(AdRequestData.class));
    }

    @Test
    public void insertAdsFetchesInlayAdsWithKruxSegmentsIfSegmentsWereReturned() {
        final Optional<String> segments = Optional.of("123-321");
        when(featureOperations.shouldRequestAds()).thenReturn(true);
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(justAppInstalls());
        when(adsOperations.kruxSegments()).thenReturn(Observable.just(segments));

        ArgumentCaptor<AdRequestData> captor = ArgumentCaptor.forClass(AdRequestData.class);

        controller.insertAds();

        verify(adsOperations).inlaysAds(captor.capture());
        assertThat(captor.getValue().getKruxSegments()).isEqualTo(segments);
    }

    @Test
    public void insertAdsFetchesInlayAds() {
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(justAppInstalls());

        controller.insertAds();

        verify(adsOperations).inlaysAds(any(AdRequestData.class));
    }

    @Test
    public void insertAdsDoesNotFetchInlayAdsIfItAlreadyHasAds() {
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(justAppInstalls());

        controller.insertAds();
        controller.insertAds();

        verify(adsOperations, times(1)).inlaysAds(any(AdRequestData.class));
    }

    @Test
    public void insertAdsSendsAdDeliveryEventForSuccessfullyInsertedAd() {
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(justAppInstalls());
        when(inlayAdHelper.insertAd(any(AppInstallAd.class), anyBoolean())).thenReturn(true);

        controller.insertAds();

        final TrackingEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent).isInstanceOf(AdDeliveryEvent.class);
    }

    @Test
    public void insertAdsSendsAdDeliveryEventsWithSameRequestIdForSubsequentAdInserts() {
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(justAppInstalls());
        when(inlayAdHelper.insertAd(any(AppInstallAd.class), anyBoolean())).thenReturn(true);

        controller.insertAds();
        controller.insertAds();

        final TrackingEvent firstEventOn = eventBus.firstEventOn(EventQueue.TRACKING);
        final TrackingEvent lastEventOn = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(firstEventOn).isInstanceOf(AdDeliveryEvent.class);
        assertThat(lastEventOn).isInstanceOf(AdDeliveryEvent.class);
        final AdDeliveryEvent firstEvent = (AdDeliveryEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        final AdDeliveryEvent secondEvent = (AdDeliveryEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(firstEvent.adRequestId()).isEqualTo(secondEvent.adRequestId());
    }

    @Test
    public void insertAdsAttemptsToInsertAdAfterFetch() {
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(justAppInstalls());
        when(inlayAdHelper.insertAd(any(AppInstallAd.class), anyBoolean())).thenReturn(true);

        controller.insertAds();
        controller.insertAds();

        verify(inlayAdHelper).insertAd(appInstalls.get(0), SCROLLING_DOWN);
        verify(inlayAdHelper).insertAd(appInstalls.get(1), SCROLLING_DOWN);
    }

    @Test
    public void insertAdsAttemptsToReinsertAdAfterFetchIfPreviousInsertFailed() {
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(justAppInstalls());
        when(inlayAdHelper.insertAd(any(AppInstallAd.class), anyBoolean())).thenReturn(false);

        controller.insertAds();
        controller.insertAds();

        verify(inlayAdHelper, times(2)).insertAd(appInstalls.get(0), SCROLLING_DOWN);
    }

    @Test
    public void insertsAdsWithScrollingUpIfOnScrolledWasCalledWithNegativeDy() {
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(justAppInstalls());

        controller.onScrolled(recycler, 0, -1);
        controller.insertAds();

        verify(inlayAdHelper).insertAd(appInstalls.get(0), SCROLLING_UP);
    }

    @Test
    public void insertAdsWillNotRequestMoreAdsForBackoffDurationIfReceivedNoAds() {
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(justEmpty());
        when(dateProvider.getCurrentTime()).thenReturn(0L, 30 * 1000L);

        controller.insertAds();
        controller.insertAds();
        verify(adsOperations, times(1)).inlaysAds(any(AdRequestData.class));
    }

    @Test
    public void insertAdsWillRequestMoreAdsAfterBackoffDurationIfReceivedNoAds() {
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(justEmpty());
        when(dateProvider.getCurrentTime()).thenReturn(0L, 61 * 1000L);

        controller.insertAds();
        controller.insertAds();
        verify(adsOperations, times(2)).inlaysAds(any(AdRequestData.class));
    }

    @Test
    public void insertAdsWillNotRequestMoreAdsBeforeBackoffDurationIfReceivedError() {
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(Observable.error(new IllegalStateException("400")));
        when(dateProvider.getCurrentTime()).thenReturn(0L, 30 * 1000L);

        controller.insertAds();
        controller.insertAds();
        verify(adsOperations, times(1)).inlaysAds(any(AdRequestData.class));
    }

    @Test
    public void insertAdsWillRequestMoreAdsAfterBackoffDurationIfReceivedError() {
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(Observable.error(new IllegalStateException("400")));
        when(dateProvider.getCurrentTime()).thenReturn(0L, 61 * 1000L);

        controller.insertAds();
        controller.insertAds();
        verify(adsOperations, times(2)).inlaysAds(any(AdRequestData.class));
    }

    @Test
    public void insertAdsWillNeverInsertAnExpiredAd() {
        final long createdAtMs = appInstalls.get(1).getCreatedAt();
        final long expiryTimeMs = appInstalls.get(1).getExpiryInMins() * 60 * 1000L;
        when(dateProvider.getCurrentTime()).thenReturn(createdAtMs + expiryTimeMs);
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(justAppInstalls());

        controller.insertAds();

        verify(inlayAdHelper, never()).insertAd(any(AppInstallAd.class), anyBoolean());
    }

    @Test
    public void insertAdsWillInsertAnAdIfNotExpired() {
        final long createdAtMs = appInstalls.get(0).getCreatedAt();
        final long justBeforeExpiryMs = appInstalls.get(0).getExpiryInMins() * 59 * 1000L;
        when(dateProvider.getCurrentTime()).thenReturn(createdAtMs + justBeforeExpiryMs);
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(justAppInstalls());

        controller.insertAds();

        verify(inlayAdHelper).insertAd(appInstalls.get(0), SCROLLING_DOWN);
    }

    @Test
    public void fetchAdsDoesNotFetchInlayAdsIfItAlreadyHasAds() {
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(justAppInstalls());
        controller.insertAds();
        controller.insertAds();
        verify(adsOperations, times(1)).inlaysAds(any(AdRequestData.class));
    }

    @Test
    public void fetchAdsDoesNotFetchInlayAdsWithoutFetchAdsFeature() {
        when(featureOperations.shouldRequestAds()).thenReturn(false);
        controller.insertAds();
        verify(adsOperations, never()).inlaysAds(any(AdRequestData.class));
    }

    @Test
    public void fetchAdsFetchesInlayAdsIfUserHasFetchAdsFeature() {
        when(adsOperations.inlaysAds(any(AdRequestData.class))).thenReturn(justAppInstalls());
        controller.insertAds();
        verify(adsOperations).inlaysAds(any(AdRequestData.class));
    }

    private Observable<List<AppInstallAd>> justAppInstalls() {
        final List<AppInstallAd> installs = new ArrayList<>(appInstalls);
        return Observable.just(installs);
    }

    private Observable<List<AppInstallAd>> justEmpty() {
        return Observable.just(Collections.<AppInstallAd>emptyList());
    }
}
