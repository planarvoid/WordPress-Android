package com.soundcloud.android.playback.ui;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;

@RunWith(SoundCloudTestRunner.class)
public class AdPagePresenterTest {

    private AdPagePresenter presenter;

    private View adView;
    @Mock private ImageOperations imageOperations;
    @Mock private PlayerOverlayController playerOverlayController;
    @Mock private AdPageListener pageListener;

    @Before
    public void setUp() throws Exception {
        presenter = new AdPagePresenter(imageOperations, Robolectric.application.getResources(), playerOverlayController, pageListener);
        adView = presenter.createItemView(new FrameLayout(Robolectric.application));
        presenter.bindItemView(adView, buildAd());
    }

    @Test
    public void shouldHideOverlayOnPlayingState() {
        presenter.setPlayState(adView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, TrackUrn.NOT_SET), true);

        verify(playerOverlayController).hideOverlay(adView.findViewById(R.id.artwork_overlay));
    }

    @Test
    public void shouldShowOverlayOnIdleState() {
        presenter.setPlayState(adView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE, TrackUrn.NOT_SET), true);

        verify(playerOverlayController).darken(adView.findViewById(R.id.artwork_overlay));
    }

    @Test
    public void togglePlayOnFooterToggleClick() {
        adView.findViewById(R.id.footer_toggle).performClick();

        verify(pageListener).onTogglePlay();
    }

    @Test
    public void togglePlayOnPlayClick() {
        adView.findViewById(R.id.player_play).performClick();

        verify(pageListener).onTogglePlay();
    }

    @Test
    public void togglePlayOnAdPageArtworkClick() {
        adView.findViewById(R.id.track_page_artwork).performClick();

        verify(pageListener).onTogglePlay();
    }

    @Test
    public void togglePlayOnAdPageArtworkOverlayClick() {
        adView.findViewById(R.id.artwork_overlay).performClick();

        verify(pageListener).onTogglePlay();
    }

    @Test
    public void clickThroughOnLearnMoreClick() {
        adView.findViewById(R.id.learn_more).performClick();

        verify(pageListener).onClickThrough();
    }

    @Test
    public void showAboutAdsOnWhyAdsClick() {
        adView.findViewById(R.id.why_ads).performClick();

        verify(pageListener).onAboutAds();
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionOnClickingUnexpectedView() {
        presenter.onClick(mock(View.class));
    }

    private PropertySet buildAd() {
        return PropertySet.from(
                PlayableProperty.CREATOR_NAME.bind("Advertiser"),
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                AdProperty.ARTWORK.bind(Uri.EMPTY),
                AdProperty.CLICK_THROUGH_LINK.bind(Uri.EMPTY)
        );
    }
}