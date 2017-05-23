package com.soundcloud.android.ads;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.DefaultImageListener;
import com.soundcloud.android.image.ImageOperations;

import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

class SponsoredSessionCardView {

    private final ImageOperations imageOperations;

    interface Listener {
        void onOptInClick();
        void onOptOutClick();
    }

    @Inject
    SponsoredSessionCardView(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    public void setupContentView(View pageView, SponsoredSessionAd ad, Listener listener) {
        final Holder holder = new Holder(pageView);

        imageOperations.displayAdImage(ad.adUrn(), ad.optInCard().imageUrl(), holder.imageView, new SponsoredSessionAdListener(ad, listener));

        holder.optInButton.setOnClickListener(btnView -> listener.onOptInClick());
        holder.optOutButton.setOnClickListener(btnView -> listener.onOptOutClick());
    }

    private final class SponsoredSessionAdListener extends DefaultImageListener {

        private final SponsoredSessionAd ad;
        private final Listener listener;

        SponsoredSessionAdListener(SponsoredSessionAd ad, Listener listener) {
            this.ad = ad;
            this.listener = listener;
        }
    }

    static class Holder {
        @BindView(R.id.ad_image_view) ImageView imageView;
        @BindView(R.id.btn_right) View optInButton;
        @BindView(R.id.btn_left) View optOutButton;

        Holder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
