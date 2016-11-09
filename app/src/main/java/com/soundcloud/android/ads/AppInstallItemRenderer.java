package com.soundcloud.android.ads;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.stream.StreamItem.AppInstall;
import com.soundcloud.android.util.CondensedNumberFormatter;

import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;


public class AppInstallItemRenderer implements CellRenderer<StreamItem> {

    private final Resources resources;
    private final CondensedNumberFormatter numberFormatter;
    private final ImageOperations imageOperations;

    public interface Listener {
        void onAppInstallItemClicked(Context context, AppInstallAd appInstallAd);
        void onWhyAdsClicked(Context context);
    }

    private Listener listener;

    @Inject
    public AppInstallItemRenderer(Resources resources,
                                  CondensedNumberFormatter numberFormatter,
                                  ImageOperations imageOperations) {
        this.resources = resources;
        this.numberFormatter = numberFormatter;
        this.imageOperations = imageOperations;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View adView = LayoutInflater.from(parent.getContext())
                                          .inflate(R.layout.stream_app_install_card, parent, false);
        adView.setTag(new Holder(adView));
        return adView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<StreamItem> items) {
        final AppInstallAd appInstall = ((AppInstall) items.get(position)).appInstall();
        final Holder holder = getHolder(itemView);

        imageOperations.displayAppInstall(appInstall.getAdUrn(), appInstall.getImageUrl(), holder.image);

        holder.headerText.setText(getSponsoredHeaderText());
        holder.appNameText.setText(appInstall.getName());
        holder.ratingsCount.setText(resources.getQuantityString(R.plurals.ads_app_ratings,
                                                       appInstall.getRatersCount(),
                                                       numberFormatter.format(appInstall.getRatersCount())));
        holder.callToAction.setText(appInstall.getCtaButtonText());
        holder.ratingBar.setRating(appInstall.getRating());

        bindWhyAdsListener(holder);
        bindClickthroughListener(holder, appInstall);
    }

    private void bindWhyAdsListener(Holder holder) {
        holder.whyAds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onWhyAdsClicked(view.getContext());
                }
            }
        });
    }

    private void bindClickthroughListener(Holder holder, final AppInstallAd appInstallAd) {
        final View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onAppInstallItemClicked(view.getContext(), appInstallAd);
                }
            }
        };
        holder.callToAction.setOnClickListener(clickListener);
        holder.image.setOnClickListener(clickListener);
    }

    private SpannableString getSponsoredHeaderText() {
        final String itemType = resources.getString(R.string.ads_app);
        final String headerText = resources.getString(R.string.stream_sponsored_item, itemType);
        final SpannableString spannedString = new SpannableString(headerText);

        spannedString.setSpan(new ForegroundColorSpan(resources.getColor(R.color.list_secondary)),
                              0,
                              headerText.length() - itemType.length(),
                              Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannedString;
    }

    private Holder getHolder(View adView) {
        return (Holder) adView.getTag();
    }

    static class Holder {
        @Bind(R.id.ad_item) TextView headerText;
        @Bind(R.id.app_name) TextView appNameText;
        @Bind(R.id.ratings_count) TextView ratingsCount;
        @Bind(R.id.call_to_action) TextView callToAction;
        @Bind(R.id.image) ImageView image;
        @Bind(R.id.rating_bar) RatingBar ratingBar;
        @Bind(R.id.why_ads) TextView whyAds;

        Holder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
