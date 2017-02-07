package com.soundcloud.android.ads;

import android.content.res.Resources;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.soundcloud.android.R;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.AspectRatioImageView;
import com.soundcloud.android.view.AspectRatioTextureView;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;

import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;

public class VideoAdItemRendererTest extends AndroidUnitTest {

    private static final List<VideoAd> VIDEOS = Collections.singletonList(AdFixtures.getVideoAd(32L));
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

        verify(listener).onVideoTextureBind((TextureView) adView.findViewById(R.id.video_view), VIDEOS.get(0));
    }

    @Test
    public void onVideoTextureBindCalledOnViewAttached() {
        renderer.onViewAttachedToWindow(adView, Optional.of(VIDEOS.get(0)));

        verify(listener).onVideoTextureBind((TextureView) adView.findViewById(R.id.video_view), VIDEOS.get(0));
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

        adView.findViewById(R.id.call_to_action).performClick();

        verify(listener).onAdItemClicked(adView.getContext(), VIDEOS.get(0));
    }
}
