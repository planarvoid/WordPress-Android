package com.soundcloud.android.ads;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;

import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

class SponsoredSessionCardView extends PrestitialView {

    private final ImageOperations imageOperations;

    @BindView(R.id.ad_image_view) ImageView imageView;
    @BindView(R.id.btn_right) View optInButton;
    @BindView(R.id.btn_left) View optOutButton;

    @Inject
    SponsoredSessionCardView(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    public void setupContentView(View view, SponsoredSessionAd ad, Listener listener) {
        ButterKnife.bind(this, view);

        imageOperations.displayAdImage(ad.adUrn(), ad.optInCard().imageUrl(), imageView, new PrestitialImageCompanionListener(ad, listener));

        optInButton.setOnClickListener(ignored -> listener.onOptInClick());
        optOutButton.setOnClickListener(ignored -> listener.closePrestitial());
    }
}
