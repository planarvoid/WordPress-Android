package com.soundcloud.android.ads;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

class VisualPrestitialView extends PrestitialView {

    private final ImageOperations imageOperations;

    @BindView(R.id.ad_image_view) ImageView imageView;
    @BindView(R.id.btn_continue) View continueButton;

    @Inject
    VisualPrestitialView(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    public void setupContentView(AppCompatActivity activity, VisualPrestitialAd ad, Listener listener) {
        ButterKnife.bind(this, activity);

        imageOperations.displayAdImage(ad.adUrn(), ad.imageUrl(), imageView, new PrestitialImageCompanionListener(ad, listener));

        continueButton.setOnClickListener(btnView -> listener.onContinueClick());
        imageView.setOnClickListener(imageView -> listener.onClickThrough(imageView, ad));
    }
}
