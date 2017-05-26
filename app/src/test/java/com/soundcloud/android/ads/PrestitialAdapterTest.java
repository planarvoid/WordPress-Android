package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.ads.PrestitialAdapter.PrestitialPage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v4.view.ViewPager;
import android.view.ViewGroup;

public class PrestitialAdapterTest extends AndroidUnitTest {

    @Mock SponsoredSessionCardView sponsoredSessionCardView;
    @Mock SponsoredSessionVideoView sponsoredSessionVideoView;
    @Mock PrestitialView.Listener listener;

    private PrestitialAdapter adapter;
    private SponsoredSessionAd sponsoredSessionAd;

    @Before
    public void setUp() {
        sponsoredSessionAd = AdFixtures.sponsoredSessionAd();

        adapter = new PrestitialAdapter(sponsoredSessionAd, listener, sponsoredSessionVideoView, sponsoredSessionCardView) ;
    }

    @Test
    public void setsThreePagesForSponsoredSession() {
        assertThat(adapter.getCount()).isEqualTo(3);
    }

    @Test
    public void setsUpOptInCardViewInViewPager() {
        final ViewPager containerSpy = spy(new ViewPager(context()));
        assertThat(adapter.instantiateItem(containerSpy, 0)).isNotNull();
        verify(sponsoredSessionCardView).setupContentView(any(ViewGroup.class), eq(sponsoredSessionAd), eq(listener), eq(PrestitialPage.OPT_IN_CARD));
        verify(containerSpy).addView(any(ViewGroup.class));
    }

    @Test
    public void setsUpVideoViewInViewPager() {
        final ViewPager containerSpy = spy(new ViewPager(context()));
        assertThat(adapter.instantiateItem(containerSpy, 1)).isNotNull();
        verify(sponsoredSessionVideoView).setupContentView(any(ViewGroup.class), eq(sponsoredSessionAd), eq(listener));
        verify(containerSpy).addView(any(ViewGroup.class));
    }

    @Test
    public void setsUpEndCardViewInViewPager() {
        final ViewPager containerSpy = spy(new ViewPager(context()));
        assertThat(adapter.instantiateItem(containerSpy, 2)).isNotNull();
        verify(sponsoredSessionCardView).setupContentView(any(ViewGroup.class), eq(sponsoredSessionAd), eq(listener), eq(PrestitialPage.END_CARD));
        verify(containerSpy).addView(any(ViewGroup.class));
    }
}
