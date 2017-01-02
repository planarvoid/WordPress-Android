package com.soundcloud.android.ads;

import com.soundcloud.android.events.InlayAdEvent;
import com.soundcloud.android.events.InlayAdImpressionEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class InlayAdOperationsTest extends AndroidUnitTest {
    private static AppInstallAd appInstall() {
        return AppInstallAd.create(AdFixtures.getApiAppInstall(), 424242);
    }

    private InlayAdOperations operations;
    private InlayAdOperations.OnScreenAndImageLoaded filter;

    @Mock InlayAdHelper inlayAdHelper;
    @Spy EventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        operations = new InlayAdOperations(eventBus);
        filter = new InlayAdOperations.OnScreenAndImageLoaded(inlayAdHelper);
    }

    @Test
    public void trackImpressionsIncludesOnScreenEventsWithImageLoadedBeforeEvent() {
        final Date beforeLoad = new Date(99);
        final Date loadTime = new Date(100);
        final Date afterLoad = new Date(101);

        final AppInstallAd unloaded = appInstall();
        final AppInstallAd loaded = appInstall();
        loaded.setImageLoadTimeOnce(loadTime);

        assertThat(filter.call(InlayAdEvent.OnScreen.create(42, loaded, beforeLoad))).isFalse();
        assertThat(filter.call(InlayAdEvent.OnScreen.create(42, unloaded, afterLoad))).isFalse();
        assertThat(filter.call(InlayAdEvent.OnScreen.create(42, loaded, afterLoad))).isTrue();
    }

    @Test
    public void trackImpressionsIncludesImageLoadedEventsForAdsOnScreen() {
        final int onScreen = 42;
        final int offScreen = 43;
        final AppInstallAd ad = appInstall();

        when(inlayAdHelper.isOnScreen(onScreen)).thenReturn(true);
        when(inlayAdHelper.isOnScreen(offScreen)).thenReturn(false);

        assertThat(filter.call(InlayAdEvent.ImageLoaded.create(onScreen, ad, new Date(1)))).isTrue();
        assertThat(filter.call(InlayAdEvent.ImageLoaded.create(offScreen, ad, new Date(2)))).isFalse();
    }

    @Test
    public void toImpressionSetsImpressionReported() {
        final AppInstallAd ad = appInstall();

        final InlayAdImpressionEvent impression = operations.TO_IMPRESSION.call(InlayAdEvent.ImageLoaded.create(42, ad, new Date(999)));

        assertThat(impression.getAd()).isEqualTo(ad.getAdUrn());
        assertThat(impression.getContextPosition()).isEqualTo(42);
        assertThat(impression.getTimestamp()).isEqualTo(999);
    }
}