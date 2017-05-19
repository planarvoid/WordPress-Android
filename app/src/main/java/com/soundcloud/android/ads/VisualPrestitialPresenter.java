package com.soundcloud.android.ads;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.DefaultImageListener;
import com.soundcloud.android.image.ImageOperations;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

class VisualPrestitialPresenter {

    private final ImageOperations imageOperations;

    interface Listener {
        void onClickThrough(View view, VisualPrestitialAd ad);
        void onImageLoadComplete(VisualPrestitialAd ad, View imageView);
        void onContinueClick();
    }

    @Inject
    VisualPrestitialPresenter(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    public void setupContentView(View pageView, VisualPrestitialAd ad, Listener listener) {
        final Holder holder = new Holder(pageView);

        imageOperations.displayAdImage(ad.adUrn(), ad.imageUrl(), holder.imageView, new VisualPrestitialAdListener(ad, listener));

        holder.continueButton.setOnClickListener(view -> listener.onContinueClick());
        holder.imageView.setOnClickListener(view -> listener.onClickThrough(view, ad));
    }

    private final class VisualPrestitialAdListener extends DefaultImageListener {

        private final VisualPrestitialAd ad;
        private final Listener listener;

        VisualPrestitialAdListener(VisualPrestitialAd ad, Listener listener) {
            this.ad = ad;
            this.listener = listener;
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            listener.onImageLoadComplete(ad, view);
        }
    }

    static class Holder {
        @BindView(R.id.ad_image_view) ImageView imageView;
        @BindView(R.id.btn_continue) View continueButton;

        Holder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
