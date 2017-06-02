package com.soundcloud.android.ads;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.image.DefaultImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

public class VisualPrestitialViewTest extends AndroidUnitTest {

    @Mock ImageOperations imageOperations;
    @Mock VisualPrestitialView.Listener listener;

    private VisualPrestitialView view;
    private VisualPrestitialAd ad;
    private AppCompatActivity activity;

    @Before
    public void setUp() {
        activity = activity();
        activity.setContentView(R.layout.visual_prestitial);
        view = new VisualPrestitialView(imageOperations);
        ad = AdFixtures.visualPrestitialAd();
    }

    @Test
    public void displaysPrestitialAdImageOnSetupWithVisualPresitialAdListener() {
        view.setupContentView(activity, ad, listener);
        final Urn adUrn = ad.adUrn();
        final String imageUrl = ad.imageUrl();

        verify(imageOperations).displayAdImage(eq(adUrn),
                                               eq(imageUrl),
                                               any(ImageView.class),
                                               any(DefaultImageListener.class));
    }

    @Test
    public void finishesActivityOnClickOfContinueButton(){
        view.setupContentView(activity, ad, listener);

        view.continueButton.performClick();

        verify(listener).onContinueClick();
    }

    @Test
    public void notifiesListenerOfClickThrough(){
        view.setupContentView(activity, ad, listener);

        final ImageView imageView = view.imageView;
        imageView.performClick();

        verify(listener).onImageClick(imageView.getContext(), ad, Optional.absent());
    }
}
