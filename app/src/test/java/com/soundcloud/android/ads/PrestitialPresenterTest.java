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
    @Mock VisualPrestitialPresenter visualPrestitialPresenter;
    @Mock PrestitialAdsController adsController;
    @Mock PrestitialAdapterFactory adapterFactory;
    @Mock PrestitialAdapter adapter;

    @Mock Intent intent;
    @Mock ImageView imageView;

    private TestEventBus eventBus;
    private VisualPrestitialAd ad;
    private AppCompatActivity activity;
    private PrestitialPresenter presenter;

    @Before
    public void setUp() {
        activity = activity();
        activity.setContentView(R.layout.prestitial);
        ad = AdFixtures.visualPrestitialAd();
        eventBus = new TestEventBus();
        presenter = new PrestitialPresenter(adsController,
                                            viewabilityController,
                                            adapterFactory,
                                            navigator,
                                            eventBus);
        when(adsController.getCurrentAd()).thenReturn(Optional.of(ad));
    }

    @Test
    public void finishesActivityOnContinueClick() {
        final AppCompatActivity activitySpy = spy(activity);
        presenter.onCreate(activitySpy, null);

        presenter.onContinueClick();

        verify(activitySpy).finish();
    }

    @Test
    public void setsUpViewAndAdapterOnCreateWhenAdIsPresent() {
        when(adapterFactory.create(ad, presenter)).thenReturn(adapter);

        presenter.onCreate(activity, null);

        assertThat(presenter.pager.getAdapter()).isEqualTo(adapter);
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

        presenter.onClickThrough(imageView, ad);

        verify(navigator).openAdClickthrough(context(), ad.clickthroughUrl());
    }

    @Test
    public void publishesUIEventOnClickThrough() {
        final View imageView = new View(context());
        presenter.onCreate(activity, null);

        presenter.onClickThrough(imageView, ad);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(UIEvent.class);
    }

    @Test
    public void finishesActivityOnClickThrough() {
        final View imageView = new View(context());
        final AppCompatActivity activitySpy = spy(activity);
        presenter.onCreate(activitySpy, null);

        presenter.onClickThrough(imageView, ad);

        verify(activitySpy).finish();
    }

    @Test
    public void publishesImpressionOnImageLoadComplete(){
        presenter.onImageLoadComplete(ad, imageView);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PrestitialAdImpressionEvent.class);
    }

    @Test
    public void startsViewabilityTrackingOnImageLoadComplete(){
        presenter.onImageLoadComplete(ad, imageView);

        verify(viewabilityController).startDisplayTracking(imageView, ad);
    }

    @Test
    public void stopsViewabilityTrackingOnDestroy(){
        presenter.onDestroy(activity);

        verify(viewabilityController).stopDisplayTracking();
    }
}
