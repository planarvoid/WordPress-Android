package com.soundcloud.android.ads;

import static com.soundcloud.android.ads.PrestitialAdapter.PrestitialPage;

import com.soundcloud.android.image.DefaultImageListener;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.TextureView;
import android.view.View;

abstract class PrestitialView {

    public interface Listener {
        void onTogglePlayback();
        void onVideoTextureBind(TextureView textureView, View viewabilityLayer, VideoAd videoAd);

        void onSkipAd();
        void onWhyAdsClicked(Context context);
        void onClickThrough(View view, AdData ad);
        void onImageLoadComplete(AdData ad, View imageView, Optional<PrestitialPage> page);
        void onOptionOneClick(PrestitialPage page, SponsoredSessionAd ad, Context context);
        void onOptionTwoClick(PrestitialPage page, SponsoredSessionAd ad);
        void onContinueClick();
    }

    final class PrestitialImageCompanionListener extends DefaultImageListener {

        private final AdData ad;
        private final Listener listener;
        private final Optional<PrestitialPage> page;

        PrestitialImageCompanionListener(AdData ad, Listener listener, Optional<PrestitialPage> page) {
            this.ad = ad;
            this.listener = listener;
            this.page = page;
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            listener.onImageLoadComplete(ad, view, page);
        }
    }
}
