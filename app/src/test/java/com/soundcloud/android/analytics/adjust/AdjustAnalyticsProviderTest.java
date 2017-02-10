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
    public void tracksMidTierPurchase() {
        PurchaseEvent purchase = PurchaseEvent.forMidTierSub("5.99", "USD");

        adjustAnalyticsProvider.handleTrackingEvent(purchase);

        verify(adjustWrapper).trackPurchase(PurchaseEvent.Subscription.MID_TIER.adjustToken(), "5.99", "USD");
    }

    @Test
    public void tracksHighTierPurchase() {
        PurchaseEvent purchase = PurchaseEvent.forHighTierSub("9.99", "USD");

        adjustAnalyticsProvider.handleTrackingEvent(purchase);

        verify(adjustWrapper).trackPurchase(PurchaseEvent.Subscription.HIGH_TIER.adjustToken(), "9.99", "USD");
    }

    @Test
    public void tracksConversionImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forConversionBuyButtonImpression();

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(UpgradeFunnelEvent.AdjustToken.CONVERSION.toString());
    }

    @Test
    public void tracksPromoConversionImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forConversionPromoImpression();

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(UpgradeFunnelEvent.AdjustToken.PROMO.toString());
    }

    @Test
    public void tracksWhyAdsImpression() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forWhyAdsImpression();

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(UpgradeFunnelEvent.AdjustToken.WHY_ADS.toString());
    }

    @Test
    public void tracksHighTierTrackInPlayer() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlayerImpression(Urn.forTrack(123L));

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(UpgradeFunnelEvent.AdjustToken.HIGH_TIER_TRACK_PLAYED.toString());
    }

    @Test
    public void tracksHighTierSearchResults() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forSearchPremiumResultsImpression(Screen.SEARCH_MAIN);

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(UpgradeFunnelEvent.AdjustToken.HIGH_TIER_SEARCH_RESULTS.toString());
    }

    @Test
    public void tracksStreamUpsell() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forStreamImpression();

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(UpgradeFunnelEvent.AdjustToken.STREAM_UPSELL.toString());
    }

    @Test
    public void tracksPlaylistUpsell() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlaylistTracksImpression(Urn.forPlaylist(123L));

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(UpgradeFunnelEvent.AdjustToken.PLAYLIST_TRACKS_UPSELL.toString());
    }

    @Test
    public void tracksOfflineSettings() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forUpgradeFromSettingsImpression();

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(UpgradeFunnelEvent.AdjustToken.SETTINGS.toString());
    }

    @Test
    public void tracksPlanDowngraded() {
        UpgradeFunnelEvent event = UpgradeFunnelEvent.forResubscribeImpression();

        adjustAnalyticsProvider.handleTrackingEvent(event);

        verify(adjustWrapper).trackEvent(UpgradeFunnelEvent.AdjustToken.PLAN_DOWNGRADED.toString());
    }

}
