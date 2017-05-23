package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PrestitialAdImpressionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import dagger.Lazy;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

public class PrestitialPresenterTest extends AndroidUnitTest {

    @Mock Navigator navigator;
    @Mock AdViewabilityController viewabilityController;
    @Mock Lazy<VisualPrestitialView> lazyVisualPrestitialView;
    @Mock VisualPrestitialView visualPrestitialView;
    @Mock PrestitialAdsController adsController;
    @Mock PrestitialAdapterFactory adapterFactory;
    @Mock PrestitialAdapter adapter;

    @Mock Intent intent;
    @Mock ImageView imageView;

    private TestEventBus eventBus;
    private VisualPrestitialAd visualPrestitialAd;
    private SponsoredSessionAd sponsoredSessionAd;
    private AppCompatActivity activity;
    private PrestitialPresenter presenter;

    @Before
    public void setUp() {
        activity = activity();
        activity.setContentView(R.layout.sponsored_session_prestitial);
        visualPrestitialAd = AdFixtures.visualPrestitialAd();
        sponsoredSessionAd = AdFixtures.sponsoredSessionAd();
        eventBus = new TestEventBus();
        presenter = new PrestitialPresenter(adsController,
                                            viewabilityController,
                                            adapterFactory,
                                            lazyVisualPrestitialView,
                                            navigator,
                                            eventBus);
        when(adsController.getCurrentAd()).thenReturn(Optional.of(visualPrestitialAd));
        when(lazyVisualPrestitialView.get()).thenReturn(visualPrestitialView);
    }

    // TODO:ADS Should we/how can we test the pager adapter setup?
    @Test
    public void finishesActivityOnContinueClick() {
        final AppCompatActivity activitySpy = spy(activity);
        presenter.onCreate(activitySpy, null);

        presenter.onContinueClick();

        verify(activitySpy).finish();
    }

    @Test
    public void setsUpViewAndAdapterOnCreateWhenSponsoredSessionAdIsPresent() {
        when(adsController.getCurrentAd()).thenReturn(Optional.of(sponsoredSessionAd));
        when(adapterFactory.create(sponsoredSessionAd, presenter)).thenReturn(adapter);
        final AppCompatActivity activitySpy = spy(activity);

        presenter.onCreate(activitySpy, null);

        verify(activitySpy).setContentView(R.layout.sponsored_session_prestitial);
    }

    @Test
    public void finishesActivityWithAbsentAd() {
        when(adsController.getCurrentAd()).thenReturn(Optional.absent());
        final AppCompatActivity activitySpy = spy(activity);

        presenter.onCreate(activitySpy, null);

        verifyZeroInteractions(adapterFactory);
        verify(activitySpy).finish();
    }

    @Test
    public void navigatesToClickThroughUrlOnClickThrough() {
        final View imageView = new View(context());
        presenter.onCreate(activity, null);

        presenter.onClickThrough(imageView, visualPrestitialAd);

        verify(navigator).openAdClickthrough(context(), visualPrestitialAd.clickthroughUrl());
    }

    @Test
    public void publishesUIEventOnClickThrough() {
        final View imageView = new View(context());
        presenter.onCreate(activity, null);

        presenter.onClickThrough(imageView, visualPrestitialAd);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(UIEvent.class);
    }

    @Test
    public void finishesActivityOnClickThrough() {
        final View imageView = new View(context());
        final AppCompatActivity activitySpy = spy(activity);
        presenter.onCreate(activitySpy, null);

        presenter.onClickThrough(imageView, visualPrestitialAd);

        verify(activitySpy).finish();
    }

    @Test
    public void publishesImpressionOnImageLoadComplete(){
        presenter.onImageLoadComplete(visualPrestitialAd, imageView);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PrestitialAdImpressionEvent.class);
    }

    @Test
    public void startsViewabilityTrackingOnImageLoadComplete(){
        presenter.onImageLoadComplete(visualPrestitialAd, imageView);

        verify(viewabilityController).startDisplayTracking(imageView, visualPrestitialAd);
    }

    @Test
    public void stopsViewabilityTrackingOnDestroy(){
        presenter.onDestroy(activity);

        verify(viewabilityController).stopDisplayTracking();
    }
}
