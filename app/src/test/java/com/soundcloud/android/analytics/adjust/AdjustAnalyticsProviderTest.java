package com.soundcloud.android.analytics.adjust;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.PurchaseEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.app.Activity;

public class AdjustAnalyticsProviderTest extends AndroidUnitTest {

    @Mock private AdjustWrapper adjustWrapper;
    @Mock private Activity activity;

    private AdjustAnalyticsProvider adjustAnalyticsProvider;

    @Before
    public void setUp() throws Exception {
        adjustAnalyticsProvider = new AdjustAnalyticsProvider(adjustWrapper);
    }

    @Test
    public void tracksOnResume() {
        ActivityLifeCycleEvent event = ActivityLifeCycleEvent.forOnResume(activity);
        adjustAnalyticsProvider.handleActivityLifeCycleEvent(event);
        verify(adjustWrapper).onResume();
    }

    @Test
    public void tracksOnPause() {
        ActivityLifeCycleEvent event = ActivityLifeCycleEvent.forOnPause(activity);
        adjustAnalyticsProvider.handleActivityLifeCycleEvent(event);
        verify(adjustWrapper).onPause();
    }

    @Test
    public void tracksPurchase() {
        PurchaseEvent purchase = PurchaseEvent.forHighTierSub("9.99", "USD");

        adjustAnalyticsProvider.handleTrackingEvent(purchase);

        verify(adjustWrapper).trackPurchase("1n0o91", "9.99", "USD");
    }

    @Test
    public void tracksConversionImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forUpgradeButtonImpression();

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(AdjustEventToken.CONVERSION);
    }

    @Test
    public void tracksPromoConversionImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forUpgradePromoImpression();

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(AdjustEventToken.PROMO);
    }

    @Test
    public void tracksWhyAdsImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forWhyAdsImpression();

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(AdjustEventToken.WHY_ADS);
    }

    @Test
    public void tracksHighTierTrackInPlayer() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlayerImpression(Urn.forTrack(123L));

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(AdjustEventToken.HIGH_TIER_TRACK_PLAYED);
    }

    @Test
    public void tracksHighTierSearchResults() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forSearchPremiumResultsImpression(Screen.SEARCH_MAIN);

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(AdjustEventToken.HIGH_TIER_SEARCH_RESULTS);
    }

    @Test
    public void tracksStreamUpsell() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forStreamImpression();

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(AdjustEventToken.STREAM_UPSELL);
    }

    @Test
    public void tracksOfflineSettings() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forUpgradeFromSettingsImpression();

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(AdjustEventToken.OFFLINE_SETTINGS);
    }

    @Test
    public void tracksPlanDowngraded() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forResubscribeImpression();

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(AdjustEventToken.PLAN_DOWNGRADED);
    }

}
