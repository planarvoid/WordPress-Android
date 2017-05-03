package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PrestitialAdImpressionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

public class VisualPrestitialPresenterTest extends AndroidUnitTest {

    @Mock Navigator navigator;
    @Mock AdViewabilityController viewabilityController;
    @Mock VisualPrestitialView view;
    @Mock AppCompatActivity activity;

    @Mock Bundle bundle;
    @Mock Intent intent;
    @Mock ImageView imageView;

    private TestEventBus eventBus;
    private VisualPrestitialPresenter presenter;

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        presenter = new VisualPrestitialPresenter(view,
                                                  viewabilityController,
                                                  navigator,
                                                  eventBus);
        when(activity.getIntent()).thenReturn(intent);
        when(intent.getExtras()).thenReturn(bundle);
    }

    @Test
    public void finishesActivityWhenThereIsNoAdInBundleOnCreate() {
        when(bundle.containsKey(VisualPrestitialActivity.EXTRA_AD)).thenReturn(false);
        presenter.onCreate(activity, null);

        verify(activity).finish();
    }

    @Test
    public void setsUpViewOnCreateWhenParcelContainsVisualPrestitialAd() {
        VisualPrestitialAd ad = AdFixtures.visualPrestitialAd();
        when(bundle.containsKey(VisualPrestitialActivity.EXTRA_AD)).thenReturn(true);
        when(bundle.getParcelable(VisualPrestitialActivity.EXTRA_AD)).thenReturn(ad);

        presenter.onCreate(activity, null);

        verify(view).setupContentView(activity, ad, presenter);
    }

    @Test
    public void finishesActivityWhenParcelDoesNotContainsVisualPrestitialAd() {
        when(bundle.containsKey(VisualPrestitialActivity.EXTRA_AD)).thenReturn(true);
        when(bundle.getParcelable(VisualPrestitialActivity.EXTRA_AD)).thenReturn(new FakeAd());

        presenter.onCreate(activity, null);

        verifyZeroInteractions(view);
        verify(activity).finish();
    }

    @Test
    public void navigatesToClickThroughUrlOnClickThrough() {
        VisualPrestitialAd ad = AdFixtures.visualPrestitialAd();
        final View imageView = new View(context());

        presenter.onClickThrough(activity, imageView, ad);

        verify(navigator).openAdClickthrough(context(), ad.clickthroughUrl());
    }

    @Test
    public void publishesUIEventOnClickThrough() {
        VisualPrestitialAd ad = AdFixtures.visualPrestitialAd();
        final View imageView = new View(context());

        presenter.onClickThrough(activity, imageView, ad);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(UIEvent.class);
    }

    @Test
    public void finishesActivityOnClickThrough() {
        VisualPrestitialAd ad = AdFixtures.visualPrestitialAd();
        final View imageView = new View(context());

        presenter.onClickThrough(activity, imageView, ad);

        verify(activity).finish();
    }

    @Test
    public void publishesImpressionOnImageLoadComplete(){
        VisualPrestitialAd ad = AdFixtures.visualPrestitialAd();

        presenter.onImageLoadComplete(ad, imageView);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PrestitialAdImpressionEvent.class);
    }

    @Test
    public void startsViewabilityTrackingOnImageLoadComplete(){
        VisualPrestitialAd ad = AdFixtures.visualPrestitialAd();

        presenter.onImageLoadComplete(ad, imageView);

        verify(viewabilityController).startDisplayTracking(imageView, ad);
    }

    @Test
    public void stopsViewabilityTrackingOnDestroy(){
        presenter.onDestroy(activity);

        verify(viewabilityController).stopDisplayTracking();
    }

    private class FakeAd extends AdData implements Parcelable {
        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) { /* no-op */ }

        @Override
        public Urn adUrn() { return null; }

        @Override
        public MonetizationType monetizationType() { return null; }
    }
}
