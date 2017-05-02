package com.soundcloud.android.ads;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.DefaultImageListener;
import com.soundcloud.android.image.ImageOperations;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

class VisualPrestitialView {

    private final ImageOperations imageOperations;

    @BindView(R.id.ad_image_view) ImageView imageView;
    @BindView(R.id.btn_continue) View continueButton;

    interface Listener {
        void onClickThrough(AppCompatActivity activity, View view, VisualPrestitialAd ad);
        void onImageLoadComplete(VisualPrestitialAd ad);
    }

    @Inject
    VisualPrestitialView(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    public void setupContentView(AppCompatActivity activity, VisualPrestitialAd ad, Listener listener) {
        ButterKnife.bind(this, activity.findViewById(android.R.id.content));

        imageOperations.displayAdImage(ad.adUrn(), ad.imageUrl(), imageView, new VisualPrestitialAdListener(ad, listener));

        continueButton.setOnClickListener(view -> activity.finish());
        imageView.setOnClickListener(view -> listener.onClickThrough(activity, view, ad));
    }

    private final class VisualPrestitialAdListener extends DefaultImageListener {

        private final VisualPrestitialAd ad;
        private final Listener listener;

        private VisualPrestitialAdListener(VisualPrestitialAd ad, Listener listener) {
            this.ad = ad;
            this.listener = listener;
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            listener.onImageLoadComplete(ad);
        }
    }
}
