package com.soundcloud.android.ads;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.interstitialForPlayer;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Expect;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;

@RunWith(SoundCloudTestRunner.class)
public class InterstitialPresenterTest {

    private PropertySet properties;
    private InterstitialPresenter presenter;
    AdOverlayPresenter adOverlayPresenter;

    @Mock AdOverlayPresenter.Listener listener;
    @Mock View trackView;
    @Mock private View overlay;
    @Mock private View closeView;
    @Mock private View imageHolder;
    @Mock private ImageView imageView;
    @Mock private ViewStub overlayStub;
    @Mock private EventBus eventBus;
    @Mock private ImageOperations imageOperations;
    @Mock private Resources resources;

    @Before
    public void setUp() {
        when(trackView.findViewById(R.id.interstitial_stub)).thenReturn(overlayStub);
        when(overlayStub.inflate()).thenReturn(overlay);
        when(overlay.findViewById(R.id.interstitial_close)).thenReturn(closeView);
        when(overlay.findViewById(R.id.interstitial_image)).thenReturn(imageView);
        when(overlay.findViewById(R.id.interstitial_image_holder)).thenReturn(imageHolder);
        when(resources.getBoolean(R.bool.allow_interstitials)).thenReturn(true);

        properties = interstitialForPlayer();
        presenter = new InterstitialPresenter(trackView, listener, eventBus, imageOperations, resources);
    }

    @Test
    public void createsInterstitialPresenterFromInterstitialPropertySet() throws Exception {
        adOverlayPresenter = AdOverlayPresenter.create(TestPropertySets.interstitialForPlayer(), trackView, listener, eventBus, Robolectric.application.getResources(), imageOperations);
        Expect.expect(adOverlayPresenter).toBeInstanceOf(InterstitialPresenter.class);
    }

    @Test
    public void doesNotShowInterstitialIfInBackground() {
        final boolean shouldDisplay = presenter.shouldDisplayOverlay(properties, true, true, false);

        expect(shouldDisplay).toBeFalse();
    }

    @Test
    public void doesNotShowInterstitialIfCollapsed() {
        final boolean shouldDisplay = presenter.shouldDisplayOverlay(properties, false, true, true);

        expect(shouldDisplay).toBeFalse();
    }

    @Test
    public void doesNotShowInterstitialIfAlreadyDismissed() {
        properties.put(AdOverlayProperty.META_AD_DISMISSED, true);
        final boolean shouldDisplay = presenter.shouldDisplayOverlay(properties, true, true, true);

        expect(shouldDisplay).toBeFalse();
    }

    @Test
    public void doesNotShowInterstitialIfResourcesDoesNotAllow() {
        when(resources.getBoolean(R.bool.allow_interstitials)).thenReturn(false);

        final boolean shouldDisplay = presenter.shouldDisplayOverlay(properties, true, true, true);
        expect(shouldDisplay).toBeFalse();
    }

    @Test
    public void showsInterstitials() {
        final boolean shouldDisplay = presenter.shouldDisplayOverlay(properties, true, true, true);

        expect(shouldDisplay).toBeTrue();
    }

    @Test
    public void isFullScreen() throws Exception {
        expect(presenter.isFullScreen()).toBeTrue();
    }
}