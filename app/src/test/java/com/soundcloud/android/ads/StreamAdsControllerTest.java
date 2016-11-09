package com.soundcloud.android.ads;

import android.support.v7.widget.RecyclerView;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.CurrentDateProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StreamAdsControllerTest extends AndroidUnitTest {

    private static final List<AppInstallAd> appInstalls = AdFixtures.getAppInstalls();
    private static final boolean SCROLLING_DOWN = false;
    private static final boolean SCROLLING_UP = true;

    @Mock private AdsOperations adsOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private FeatureOperations featureOperations;
    @Mock private CurrentDateProvider dateProvider;
    @Mock private InlayAdInsertionHelper insertionHelper;
    @Mock private RecyclerView recycler;

    private StreamAdsController controller;

    @Before
    public void setUp() {
        controller = spy(new StreamAdsController(adsOperations, featureFlags, featureOperations, dateProvider));
        controller.set(insertionHelper);

        when(featureFlags.isEnabled(Flag.APP_INSTALLS)).thenReturn(true);
        when(featureOperations.shouldRequestAds()).thenReturn(true);
    }

    @Test
    public void onScrollStateChangedInsertsAdsOnSettling() {
        when(featureFlags.isEnabled(Flag.APP_INSTALLS)).thenReturn(false);

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
    public void insertAdsDoesNotFetchInlayAdsIfUserHasShouldntFetchAdsFeature() {
        when(featureOperations.shouldRequestAds()).thenReturn(false);

        controller.insertAds();

        verify(adsOperations, never()).inlaysAds();
    }

    @Test
    public void insertAdsFetchesInlayAdsIfUserHasFetchAdsFeature() {
        when(featureOperations.shouldRequestAds()).thenReturn(true);
        when(adsOperations.inlaysAds()).thenReturn(justAppInstalls());

        controller.insertAds();

        verify(adsOperations).inlaysAds();
    }

    @Test
    public void insertAdsDoesNotFetchInlayAdsIfFeatureDisabled() {
        when(featureFlags.isEnabled(Flag.APP_INSTALLS)).thenReturn(false);

        controller.insertAds();

        verify(adsOperations, never()).inlaysAds();
    }

    @Test
    public void insertAdsFetchesInlayAds() {
        when(adsOperations.inlaysAds()).thenReturn(justAppInstalls());

        controller.insertAds();

        verify(adsOperations).inlaysAds();
    }

    @Test
    public void insertAdsDoesNotFetchInlayAdsIfItAlreadyHasAds() {
        when(adsOperations.inlaysAds()).thenReturn(justAppInstalls());

        controller.insertAds();
        controller.insertAds();

        verify(adsOperations, times(1)).inlaysAds();
    }

    @Test
    public void insertAdsAttemptsToInsertAdAfterFetch() {
        when(adsOperations.inlaysAds()).thenReturn(justAppInstalls());
        when(insertionHelper.insertAd(any(AppInstallAd.class), anyBoolean())).thenReturn(true);

        controller.insertAds();
        controller.insertAds();

        verify(insertionHelper).insertAd(appInstalls.get(0), SCROLLING_DOWN);
        verify(insertionHelper).insertAd(appInstalls.get(1), SCROLLING_DOWN);
    }

    @Test
    public void insertAdsAttemptsToReinsertAdAfterFetchIfPreviousInsertFailed() {
        when(adsOperations.inlaysAds()).thenReturn(justAppInstalls());
        when(insertionHelper.insertAd(any(AppInstallAd.class), anyBoolean())).thenReturn(false);

        controller.insertAds();
        controller.insertAds();

        verify(insertionHelper, times(2)).insertAd(appInstalls.get(0), SCROLLING_DOWN);
    }

    @Test
    public void insertsAdsWithScrollingUpIfOnScrolledWasCalledWithNegativeDy() {
        when(adsOperations.inlaysAds()).thenReturn(justAppInstalls());

        controller.onScrolled(recycler, 0, -1);
        controller.insertAds();

        verify(insertionHelper).insertAd(appInstalls.get(0), SCROLLING_UP);
    }

    @Test
    public void insertAdsWillNotRequestMoreAdsForBackoffDurationIfReceivedNoAds() {
        when(adsOperations.inlaysAds()).thenReturn(Observable.just(Collections.<AppInstallAd>emptyList()));
        when(dateProvider.getCurrentTime()).thenReturn(0L, 30 * 1000L);

        controller.insertAds();
        controller.insertAds();
        verify(adsOperations, times(1)).inlaysAds();
    }

    @Test
    public void insertAdsWillRequestMoreAdsAfterBackoffDurationIfReceivedNoAds() {
        when(adsOperations.inlaysAds()).thenReturn(Observable.just(Collections.<AppInstallAd>emptyList()));
        when(dateProvider.getCurrentTime()).thenReturn(0L, 61 * 1000L);

        controller.insertAds();
        controller.insertAds();
        verify(adsOperations, times(2)).inlaysAds();
    }

    @Test
    public void insertAdsWillNotRequestMoreAdsBeforeBackoffDurationIfReceivedError() {
        when(adsOperations.inlaysAds()).thenReturn(Observable.<List<AppInstallAd>>error(new IllegalStateException("400")));
        when(dateProvider.getCurrentTime()).thenReturn(0L, 30 * 1000L);

        controller.insertAds();
        controller.insertAds();
        verify(adsOperations, times(1)).inlaysAds();
    }

    @Test
    public void insertAdsWillRequestMoreAdsAfterBackoffDurationIfReceivedError() {
        when(adsOperations.inlaysAds()).thenReturn(Observable.<List<AppInstallAd>>error(new IllegalStateException("400")));
        when(dateProvider.getCurrentTime()).thenReturn(0L, 61 * 1000L);

        controller.insertAds();
        controller.insertAds();
        verify(adsOperations, times(2)).inlaysAds();
    }

    @Test
    public void insertAdsWillNeverInsertAnExpiredAd() {
        final long createdAtMs = appInstalls.get(1).getCreatedAt();
        final long expiryTimeMs = appInstalls.get(1).getExpiryInMins() * 60 * 1000L;
        when(dateProvider.getCurrentTime()).thenReturn(createdAtMs + expiryTimeMs);
        when(adsOperations.inlaysAds()).thenReturn(justAppInstalls());

        controller.insertAds();

        verify(insertionHelper, never()).insertAd(any(AppInstallAd.class), anyBoolean());
    }

    @Test
    public void insertAdsWillInsertAnAdIfNotExpired() {
        final long createdAtMs = appInstalls.get(0).getCreatedAt();
        final long justBeforeExpiryMs = appInstalls.get(0).getExpiryInMins() * 59 * 1000L;
        when(dateProvider.getCurrentTime()).thenReturn(createdAtMs + justBeforeExpiryMs);
        when(adsOperations.inlaysAds()).thenReturn(justAppInstalls());

        controller.insertAds();

        verify(insertionHelper).insertAd(appInstalls.get(0), SCROLLING_DOWN);
    }

    private Observable<List<AppInstallAd>> justAppInstalls() {
        final List<AppInstallAd> installs = new ArrayList<>(appInstalls);
        return Observable.just(installs);
    }
}