package com.soundcloud.android.ads;

import android.content.res.Resources;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.InlayAdEvent;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.view.AspectRatioTextureView;
import com.soundcloud.android.view.IconToggleButton;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;

import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VideoAdItemRendererTest extends AndroidUnitTest {

    private static final VideoAd VIDEO_AD_1 = AdFixtures.getInlayVideoAd(32L);
    private static final VideoAd VIDEO_AD_2 = VideoAd.create(AdFixtures.getApiVideoAd("title", "custom cta"), 1L, AdData.MonetizationType.INLAY);
    private static final List<VideoAd> VIDEOS = Arrays.asList(VIDEO_AD_1, VIDEO_AD_2);

    private static final List<StreamItem> ITEMS = Lists.transform(VIDEOS, StreamItem::forVideoAd);

    @Mock private Resources resources;
    @Mock private VideoAdItemRenderer.Listener listener;
    @Mock private CurrentDateProvider currentDateProvider;

    private TestEventBus eventBus;
    private VideoAdItemRenderer renderer;
    private View adView;

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        renderer = new VideoAdItemRenderer(resources(), eventBus, currentDateProvider);
        renderer.setListener(listener);
        adView = renderer.createItemView(new FrameLayout(context()));
    }

    @Test
    public void onVideoTextureBindCalledOnBind() {
        renderer.bindItemView(0, adView, ITEMS);

        verify(listener).onVideoTextureBind((TextureView) adView.findViewById(R.id.video_view), VIDEO_AD_1);
    }

    @Test
    public void onVideoTextureBindCalledOnViewAttached() {
        renderer.onViewAttachedToWindow(adView, Optional.of(VIDEO_AD_1));

        verify(listener).onVideoTextureBind((TextureView) adView.findViewById(R.id.video_view), VIDEO_AD_1);
    }

    @Test
    public void videoAspectRatioSetOnBind() {
        renderer.bindItemView(0, adView, ITEMS);

        AspectRatioTextureView textureView = (AspectRatioTextureView) adView.findViewById(R.id.video_view);
        assertThat(textureView.getAspectRatio()).isCloseTo(1.77f, Offset.offset(0.01f));
    }

    @Test
    public void onWhyAdsClickOpensDialog() {
        renderer.bindItemView(0, adView, ITEMS);

        adView.findViewById(R.id.why_ads).performClick();

        verify(listener).onWhyAdsClicked(adView.getContext());
    }

    @Test
    public void callToActionClickOpensClickThrough() {
        renderer.bindItemView(0, adView, ITEMS);

        adView.findViewById(R.id.call_to_action_without_title).performClick();

        verify(listener).onAdItemClicked(adView.getContext(), VIDEO_AD_1);
    }

    @Test
    public void callToActionWhenTitleExistsClickOpensClickThrough() {
        renderer.bindItemView(1, adView, ITEMS);

        adView.findViewById(R.id.call_to_action_with_title).performClick();

        verify(listener).onAdItemClicked(adView.getContext(), VIDEO_AD_2);
    }

    @Test
    public void customTitleIsSetOnBindAndIsVisible() {
        renderer.bindItemView(1, adView, ITEMS);

        final TextView titleView = (TextView) adView.findViewById(R.id.title);
        assertThat(VIDEO_AD_2.getTitle().isPresent()).isTrue();
        assertThat(titleView.getText()).isEqualTo(VIDEO_AD_2.getTitle().get());
        assertThat(titleView).isVisible();
    }

    @Test
    public void customClickThroughTextIsSetOnBindAndIsVisible() {
        renderer.bindItemView(1, adView, ITEMS);

        final TextView clickThroughButton = (TextView) adView.findViewById(R.id.call_to_action_with_title);
        assertThat(VIDEO_AD_2.getCallToActionButtonText().isPresent()).isTrue();
        assertThat(clickThroughButton.getText()).isEqualTo(VIDEO_AD_2.getCallToActionButtonText().get());
        assertThat(clickThroughButton).isVisible();
    }

    @Test
    public void titleIsNotVisibleWhenDoesNotExist() {
        renderer.bindItemView(0, adView, ITEMS);

        final View titleContainerView = adView.findViewById(R.id.footer_with_title);
        assertThat(VIDEO_AD_1.getTitle().isPresent()).isFalse();
        assertThat(titleContainerView).isNotVisible();
    }

    @Test
    public void videoViewClickEmitsToggleMuteEventForFirstClick() {
        when(currentDateProvider.getCurrentDate()).thenReturn(new Date(999));
        renderer.bindItemView(0, adView, ITEMS);

        adView.findViewById(R.id.video_view).performClick();

        assertThat(eventBus.lastEventOn(EventQueue.INLAY_AD)).isInstanceOf(InlayAdEvent.ToggleVolume.class);
    }

    @Test
    public void videoViewClickEmitsTogglePlaybackEventForSecondClick() {
        when(currentDateProvider.getCurrentDate()).thenReturn(new Date(999));
        renderer.bindItemView(0, adView, ITEMS);

        adView.findViewById(R.id.video_view).performClick();
        adView.findViewById(R.id.video_view).performClick();

        assertThat(eventBus.lastEventOn(EventQueue.INLAY_AD)).isInstanceOf(InlayAdEvent.TogglePlayback.class);
    }

    @Test
    public void videoViewClickEmitsTogglePlaybackEventIfVolumeToggleClickedFirst() {
        when(currentDateProvider.getCurrentDate()).thenReturn(new Date(999));
        renderer.bindItemView(0, adView, ITEMS);

        adView.findViewById(R.id.video_volume_control).performClick();
        adView.findViewById(R.id.video_view).performClick();

        assertThat(eventBus.lastEventOn(EventQueue.INLAY_AD)).isInstanceOf(InlayAdEvent.TogglePlayback.class);
    }

    @Test
    public void videoViewInvisibleAfterBind() {
        renderer.bindItemView(0, adView, ITEMS);

        assertThat(adView.findViewById(R.id.video_view)).isInvisible();
    }

    @Test
    public void videoViewVisibleWhenPlaybackStarts() {
        renderer.bindItemView(0, adView, ITEMS);

        renderer.setPlayState(adView, TestPlayerTransitions.playing(), false);

        assertThat(adView.findViewById(R.id.video_view)).isVisible();
    }

    @Test
    public void volumeToggleEnabledWhenNotMuted() {
        renderer.bindItemView(0, adView, ITEMS);

        renderer.setPlayState(adView, TestPlayerTransitions.idle(), false);

        final IconToggleButton view = (IconToggleButton) adView.findViewById(R.id.video_volume_control);
        assertThat(view.isChecked()).isTrue();
    }

    @Test
    public void volumeToggleDisabledWhenMuted() {
        renderer.bindItemView(0, adView, ITEMS);

        renderer.setPlayState(adView, TestPlayerTransitions.idle(), true);

        final IconToggleButton view = (IconToggleButton) adView.findViewById(R.id.video_volume_control);
        assertThat(view.isChecked()).isFalse();
    }

    @Test
    public void volumeToggleAndFullscreenButtonNotVisibleWhenPlaybackIsFinished() {
        renderer.bindItemView(0, adView, ITEMS);

        renderer.setPlayState(adView, TestPlayerTransitions.complete(), false);

        assertThat(adView.findViewById(R.id.video_volume_control)).isGone();
        assertThat(adView.findViewById(R.id.video_fullscreen_control)).isGone();
    }

    @Test
    public void playButtonVisibleWhenPlaybackPaused() {
        renderer.bindItemView(0, adView, ITEMS);

        renderer.setPlayState(adView, TestPlayerTransitions.idle(), false);

        assertThat(adView.findViewById(R.id.player_play)).isVisible();
    }

    @Test
    public void playButtonGoneWhenPlaybackPlaying() {
        renderer.bindItemView(0, adView, ITEMS);

        renderer.setPlayState(adView, TestPlayerTransitions.playing(), false);

        assertThat(adView.findViewById(R.id.player_play)).isGone();
    }

    @Test
    public void playButtonVisibleWhenPlaybackComplete() {
        renderer.bindItemView(0, adView, ITEMS);

        renderer.setPlayState(adView, TestPlayerTransitions.complete(), false);

        assertThat(adView.findViewById(R.id.player_play)).isVisible();
    }

    @Test
    public void playButtonGoneWhenBuffering() {
        renderer.bindItemView(0, adView, ITEMS);

        renderer.setPlayState(adView, TestPlayerTransitions.buffering(), false);

        assertThat(adView.findViewById(R.id.player_play)).isGone();
    }

    @Test
    public void progressIndicatorVisibleWhenBuffering() {
        renderer.bindItemView(0, adView, ITEMS);

        renderer.setPlayState(adView, TestPlayerTransitions.buffering(), false);

        assertThat(adView.findViewById(R.id.video_progress)).isVisible();
    }

    @Test
    public void progressIndicatorGoneWhenPlayingOrIdle() {
        renderer.bindItemView(0, adView, ITEMS);

        renderer.setPlayState(adView, TestPlayerTransitions.playing(), false);
        assertThat(adView.findViewById(R.id.video_progress)).isGone();
         renderer.setPlayState(adView, TestPlayerTransitions.idle(), false);
        assertThat(adView.findViewById(R.id.video_progress)).isGone();
    }
}
