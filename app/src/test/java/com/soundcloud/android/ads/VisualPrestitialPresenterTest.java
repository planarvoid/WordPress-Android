package com.soundcloud.android.ads;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.image.DefaultImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

public class VisualPrestitialPresenterTest extends AndroidUnitTest {

    @Mock ImageOperations imageOperations;
    @Mock PrestitialActivity activity;
    @Mock VisualPrestitialPresenter.Listener listener;

    private VisualPrestitialPresenter presenter;
    private ViewPager viewPager;
    private ViewGroup view;
    private VisualPrestitialAd ad;

    @Before
    public void setUp() {
        presenter = new VisualPrestitialPresenter(imageOperations);
        viewPager = new ViewPager(context());
        view = (ViewGroup) LayoutInflater.from(context()).inflate(R.layout.visual_prestitial, viewPager, false);
        ad = AdFixtures.visualPrestitialAd();
    }

    @Test
    public void displaysPrestitialAdImageOnSetupWithVisualPresitialAdListener() {
        presenter.setupContentView(view, ad, listener);
        final Urn adUrn = ad.adUrn();
        final String imageUrl = ad.imageUrl();

        verify(imageOperations).displayAdImage(eq(adUrn),
                                               eq(imageUrl),
                                               any(ImageView.class),
                                               any(DefaultImageListener.class));
    }

    @Test
    public void finishesActivityOnClickOfContinueButton(){
        presenter.setupContentView(view, ad, listener);

        view.findViewById(R.id.btn_continue).performClick();

        verify(listener).onContinueClick();
    }

    @Test
    public void notifiesListenerOfClickThrough(){
        presenter.setupContentView(view, ad, listener);

        final ImageView imageView = (ImageView) view.findViewById(R.id.ad_image_view);
        imageView.performClick();

        verify(listener).onClickThrough(imageView, ad);
    }
}
