package com.soundcloud.android.ads;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.soundcloud.android.R;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.stream.StreamItem.Video;

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

        bindWhyAdsListener(holder.whyAds);
        bindClickthroughListener(holder.callToAction, videoAd);
    }

    private Holder getHolder(View adView) {
        return (Holder) adView.getTag();
    }

    static class Holder {
        @BindView(R.id.ad_item) TextView headerText;
        @BindView(R.id.call_to_action) TextView callToAction;
        @BindView(R.id.why_ads) TextView whyAds;

        Holder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
