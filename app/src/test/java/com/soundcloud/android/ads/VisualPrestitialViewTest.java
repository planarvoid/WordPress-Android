package com.soundcloud.android.ads;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.image.DefaultImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;

public class VisualPrestitialViewTest extends AndroidUnitTest {

    @Mock ImageOperations imageOperations;
    @Mock VisualPrestitialActivity activity;
    @Mock VisualPrestitialView.Listener listener;

    private VisualPrestitialView view;

    @Before
    public void setUp() {
        view = new VisualPrestitialView(imageOperations);

        when(activity.findViewById(android.R.id.content)).thenReturn(LayoutInflater.from(context()).inflate(R.layout.visual_prestitial, null));
    }

    @Test
    public void displaysPrestitialAdImageOnSetupWithVisualPresitialAdListener() {
        final VisualPrestitialAd ad = AdFixtures.visualPrestitialAd();

        view.setupContentView(activity, ad, listener);
        final Urn adUrn = ad.adUrn();
        final String imageUrl = ad.imageUrl();

        verify(imageOperations).displayAdImage(eq(adUrn),
                                               eq(imageUrl),
                                               eq(view.imageView),
                                               any(DefaultImageListener.class));
    }

    @Test
    public void finishesActivityOnClickOfContinueButton(){
        final VisualPrestitialAd ad = AdFixtures.visualPrestitialAd();

        view.setupContentView(activity, ad, listener);

        view.continueButton.performClick();

        verify(activity).finish();
    }

    @Test
    public void notifiesListenerOfClickThrough(){
        final VisualPrestitialAd ad = AdFixtures.visualPrestitialAd();

        view.setupContentView(activity, ad, listener);

        view.imageView.performClick();

        verify(listener).onClickThrough(activity, view.imageView, ad);
    }
}
