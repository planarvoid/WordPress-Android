package com.soundcloud.android.ads;

import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.TextureView;
import android.view.View;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.java.optional.Optional;

public abstract class AdItemRenderer implements CellRenderer<StreamItem> {

    public interface Listener {
        void onAdItemClicked(Context context, AdData adData);
        void onVideoTextureBind(TextureView textureView, VideoAd videoAd);
        void onWhyAdsClicked(Context context);
    }

    protected Optional<Listener> listener = Optional.absent();

    public void setListener(Listener listener) {
        this.listener = Optional.of(listener);
    }

    void bindWhyAdsListener(View whyAdsButton) {
        whyAdsButton.setOnClickListener(view -> {
            if (listener.isPresent()) {
                listener.get().onWhyAdsClicked(view.getContext());
            }
        });
    }

    View.OnClickListener getClickthroughListener(final AdData adData) {
        return view -> {
            if (listener.isPresent()) {
                listener.get().onAdItemClicked(view.getContext(), adData);
            }
        };
    }

    SpannableString getSponsoredHeaderText(Resources resources, String itemType) {
        final String headerText = resources.getString(R.string.stream_sponsored_item, itemType);
        final SpannableString spannedString = new SpannableString(headerText);

        spannedString.setSpan(new ForegroundColorSpan(resources.getColor(R.color.list_secondary)),
                              0,
                              headerText.length() - itemType.length(),
                              Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannedString;
    }
}
