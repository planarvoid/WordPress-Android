package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v4.view.ViewPager;
import android.view.ViewGroup;

public class PrestitialAdapterTest extends AndroidUnitTest {

    @Mock VisualPrestitialPresenter presenter;
    @Mock VisualPrestitialPresenter.Listener listener;

    private PrestitialAdapter adapterWithSponsoredSession;
    private PrestitialAdapter adapterWithVisualPrestitial;
    private VisualPrestitialAd visualPrestitialAd;
    private SponsoredSessionAd sponsoredSessionAd;

    @Before
    public void setUp() {
        visualPrestitialAd = AdFixtures.visualPrestitialAd();
        sponsoredSessionAd = AdFixtures.sponsoredSessionAd();

        adapterWithVisualPrestitial = new PrestitialAdapter(visualPrestitialAd, listener, presenter);
        adapterWithSponsoredSession = new PrestitialAdapter(sponsoredSessionAd, listener, presenter);
    }

    @Test
    public void setsThreePagesForSponsoredSession() {
        assertThat(adapterWithSponsoredSession.getCount()).isEqualTo(3);
    }

    @Test
    public void setsOnePageForVisualPrestitial() {
        assertThat(adapterWithVisualPrestitial.getCount()).isEqualTo(1);
    }

    @Test
    public void setsUpVisualPrestitialViewInViewPager() {
        final ViewPager containerSpy = spy(new ViewPager(context()));
        assertThat(adapterWithVisualPrestitial.instantiateItem(containerSpy, 0)).isNotNull();
        verify(presenter).setupContentView(any(ViewGroup.class), eq(visualPrestitialAd), eq(listener));
        verify(containerSpy).addView(any(ViewGroup.class));
    }
}
