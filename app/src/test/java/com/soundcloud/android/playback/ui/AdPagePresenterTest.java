package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class AdPagePresenterTest {

    private AdPagePresenter presenter;

    private View adView;
    @Mock private ImageOperations imageOperations;
    @Mock private PlayerOverlayController playerOverlayController;
    @Mock private AdPageListener pageListener;
    @Mock private PlayerOverlayController.Factory playerOverlayControllerFactory;

    @Before
    public void setUp() throws Exception {
        presenter = new AdPagePresenter(imageOperations, Robolectric.application.getResources(), playerOverlayControllerFactory, pageListener, Robolectric.application);
        adView = presenter.createItemView(new FrameLayout(Robolectric.application));
        presenter.bindItemView(adView, buildAd());
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
    public void playerCloseOnPlayerCloseClick() {
        adView.findViewById(R.id.player_close).performClick();

        verify(pageListener).onPlayerClose();
    }

    @Test
    public void playerCloseOnPreviewContainerClick() {
        adView.findViewById(R.id.preview_container).performClick();

        verify(pageListener).onPlayerClose();
    }

    @Test
    public void footerTapOnFooterControlsClick() {
        adView.findViewById(R.id.footer_controls).performClick();

        verify(pageListener).onFooterTap();
    }

    @Test
    public void clickThroughOnLearnMoreClick() {
        adView.findViewById(R.id.learn_more).performClick();

        verify(pageListener).onClickThrough();
    }

    @Test
    public void clickSkipAdShouldSkipAd() {
        adView.findViewById(R.id.skip_ad).performClick();

        verify(pageListener).skipAd();
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

    @Test
    public void setProgressShouldInitiallySetATimerTo15sec() {
        presenter.setProgress(adView, createProgress(TimeUnit.SECONDS, 0, 30));

        expect(skipAd()).toBeGone();
        expect(timeUntilSkip()).toBeVisible();
        expect(timeUntilSkip().getText()).toEqual("15 sec.");
    }

    @Test
    public void setProgressShouldUpdateTimeUntilSkipAccordingToPosition() {
        presenter.setProgress(adView, createProgress(TimeUnit.SECONDS, 7, 30));

        expect(skipAd()).toBeGone();
        expect(timeUntilSkip()).toBeVisible();
        expect(timeUntilSkip().getText()).toEqual("8 sec.");
    }

    @Test
    public void setProgressShouldDisplayEnableSkipAdAfter15sec() {
        presenter.setProgress(adView, createProgress(TimeUnit.SECONDS, 15, 30));

        expect(timeUntilSkip()).toBeGone();
        expect(skipAd()).toBeVisible();
    }

    private PlaybackProgress createProgress(TimeUnit timeUnit, int position, int duration) {
        return new PlaybackProgress(timeUnit.toMillis(position), timeUnit.toMillis(duration));
    }

    private TextView timeUntilSkip() {
        return ((TextView) adView.findViewById(R.id.time_until_skip));
    }

    private View skipAd() {
        return adView.findViewById(R.id.skip_ad);
    }

    private PropertySet buildAd() {
        return PropertySet.from(
                PlayableProperty.TITLE.bind("Ad Title"),
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                AdProperty.ARTWORK.bind(Uri.EMPTY),
                AdProperty.CLICK_THROUGH_LINK.bind(Uri.EMPTY),
                AdProperty.MONETIZABLE_TRACK_URN.bind(Urn.forTrack(123L)),
                AdProperty.MONETIZABLE_TRACK_TITLE.bind("track"),
                AdProperty.MONETIZABLE_TRACK_CREATOR.bind("artist")
        );
    }
}