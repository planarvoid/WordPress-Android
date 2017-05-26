package com.soundcloud.android.ads;

import static com.soundcloud.android.ads.PrestitialAdapter.PrestitialPage;

import com.soundcloud.android.image.DefaultImageListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.TextureView;
import android.view.View;

abstract class PrestitialView {

    public interface Listener {
        void onTogglePlayback();
        void onVideoTextureBind(TextureView textureView, View viewabilityLayer, VideoAd videoAd);

        void onWhyAdsClicked(Context context);
        void onClickThrough(View view, AdData ad);
        void onImageLoadComplete(AdData ad, View imageView);
        void onOptionOneClick(PrestitialPage page, SponsoredSessionAd ad, Context context);
        void onOptionTwoClick(PrestitialPage page, SponsoredSessionAd ad);
        void onContinueClick();
    }

    final class PrestitialImageCompanionListener extends DefaultImageListener {

        private final AdData ad;
        private final Listener listener;

        PrestitialImageCompanionListener(AdData ad, Listener listener) {
            this.ad = ad;
            this.listener = listener;
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            listener.onImageLoadComplete(ad, view);
        }
    }
}
