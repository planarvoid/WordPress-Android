package com.soundcloud.android.ads;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.stream.StreamItem.Video;
import com.soundcloud.android.view.AspectRatioTextureView;
import com.soundcloud.java.optional.Optional;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VideoAdItemRenderer extends AdItemRenderer {

    private final Resources resources;

    @Inject
    public VideoAdItemRenderer(Resources resources) {
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View adView = LayoutInflater.from(parent.getContext())
                                          .inflate(R.layout.stream_video_ad_card, parent, false);
        adView.setTag(new Holder(adView));
        return adView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<StreamItem> items) {
        final VideoAd videoAd = ((Video) items.get(position)).video();
        final Holder holder = getHolder(itemView);

        holder.headerText.setText(getSponsoredHeaderText(resources, resources.getString(R.string.ads_video)));
        holder.callToAction.setText(resources.getText(R.string.ads_call_to_action));
        holder.videoView.setAspectRatio(getVideoProportion(videoAd));

        bindWhyAdsListener(holder.whyAds);
        bindClickthroughListener(holder.callToAction, videoAd);

        if (listener.isPresent()) {
            listener.get().onVideoTextureBind(holder.videoView, videoAd);
        }
    }

    public void onViewAttachedToWindow(View itemView, Optional<AdData> adData) {
        if (listener.isPresent() && adData.isPresent() && adData.get() instanceof VideoAd) {
            final Holder holder = getHolder(itemView);
            listener.get().onVideoTextureBind(holder.videoView, (VideoAd) adData.get());
        }
    }

    public TextureView getVideoView(View itemView) {
        return getHolder(itemView).videoView;
    }

    private float getVideoProportion(VideoAd videoAd) {
        final VideoAdSource source = videoAd.getFirstSource();
        return (float) source.getHeight() / (float) source.getWidth();
    }

    private Holder getHolder(View adView) {
        return (Holder) adView.getTag();
    }

    static class Holder {
        @BindView(R.id.ad_item) TextView headerText;
        @BindView(R.id.call_to_action) TextView callToAction;
        @BindView(R.id.why_ads) TextView whyAds;
        @BindView(R.id.video_view) AspectRatioTextureView videoView;

        Holder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
