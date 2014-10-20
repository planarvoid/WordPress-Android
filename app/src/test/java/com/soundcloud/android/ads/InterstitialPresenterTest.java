package com.soundcloud.android.ads;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.interstitialForPlayer;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Expect;
import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

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
    @Mock private View closeStub;
    @Mock private ImageView imageStub;
    @Mock private ViewStub overlayStub;


    @Before
    public void setUp() {
        when(trackView.findViewById(R.id.interstitial_stub)).thenReturn(overlayStub);
        when(overlayStub.inflate()).thenReturn(overlay);
        when(overlay.findViewById(R.id.interstitial_close)).thenReturn(closeStub);
        when(overlay.findViewById(R.id.interstitial_image)).thenReturn(imageStub);

        properties = interstitialForPlayer();
        presenter = new InterstitialPresenter(trackView, listener);
    }

    @Test
    public void createsInterstitialPresenterFromInterstitialPropertySet() throws Exception {
        adOverlayPresenter = AdOverlayPresenter.create(TestPropertySets.interstitialForPlayer(), trackView, listener);
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
    public void showsInterstitials() {
        final boolean shouldDisplay = presenter.shouldDisplayOverlay(properties, true, true, true);

        expect(shouldDisplay).toBeTrue();
    }

    @Test
    public void isFullScreen() throws Exception {
        expect(presenter.isFullScreen()).toBeTrue();
    }
}