package com.soundcloud.android.ads;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.stream.StreamItem.AppInstall;
import com.soundcloud.android.util.CondensedNumberFormatter;

import android.content.res.Resources;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class AppInstallItemRenderer implements CellRenderer<StreamItem> {

    private final Resources resources;
    private final CondensedNumberFormatter numberFormatter;
    private final ImageOperations imageOperations;

    @Inject
    public AppInstallItemRenderer(Resources resources,
                                  CondensedNumberFormatter numberFormatter,
                                  ImageOperations imageOperations) {
        this.resources = resources;
        this.numberFormatter = numberFormatter;
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                .inflate(R.layout.stream_app_install_card, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<StreamItem> items) {
        final AppInstallAd appInstall = ((AppInstall) items.get(position)).appInstall();

        final TextView headerText = (TextView) itemView.findViewById(R.id.ad_item);
        final TextView appNameText = (TextView) itemView.findViewById(R.id.app_name);
        final TextView raterCount = (TextView) itemView.findViewById(R.id.ratings_count);
        final ImageView view = (ImageView) itemView.findViewById(R.id.image);

        imageOperations.displayAppInstall(Uri.parse(appInstall.getImageUrl()), view);
        headerText.setText(getSponsoredHeaderText());
        appNameText.setText(appInstall.getName());
        raterCount.setText(resources.getQuantityString(R.plurals.ads_app_ratings,
                                                       appInstall.getRatersCount(),
                                                       numberFormatter.format(appInstall.getRatersCount())));
    }

    public SpannableString getSponsoredHeaderText() {
        final String itemType = resources.getString(R.string.ads_app);
        final String headerText = resources.getString(R.string.stream_sponsored_item, itemType);
        final SpannableString spannedString = new SpannableString(headerText);

        spannedString.setSpan(new ForegroundColorSpan(resources.getColor(R.color.list_secondary)),
                              0,
                              headerText.length() - itemType.length(),
                              Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannedString;
    }
}
