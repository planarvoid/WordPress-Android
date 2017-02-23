package com.soundcloud.android.ads;

import android.content.res.Resources;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.soundcloud.android.R;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.AspectRatioTextureView;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;

import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class VideoAdItemRendererTest extends AndroidUnitTest {

    private static final VideoAd VIDEO_AD_1 = AdFixtures.getVideoAd(32L);
    private static final VideoAd VIDEO_AD_2 = VideoAd.create(AdFixtures.getApiVideoAd("title", "custom cta"), 1L);
    private static final List<VideoAd> VIDEOS = Arrays.asList(VIDEO_AD_1, VIDEO_AD_2);

    private static final List<StreamItem> ITEMS = Lists.transform(VIDEOS, StreamItem::forVideoAd);

    @Mock private Resources resources;
    @Mock private VideoAdItemRenderer.Listener listener;

    private VideoAdItemRenderer renderer;
    private View adView;

    @Before
    public void setUp() {
        renderer = new VideoAdItemRenderer(resources());
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
}