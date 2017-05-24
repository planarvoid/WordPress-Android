package com.soundcloud.android.ads;

import com.soundcloud.android.image.DefaultImageListener;

import android.graphics.Bitmap;
import android.view.TextureView;
import android.view.View;

abstract class PrestitialView {

    public interface Listener {
        void onVideoTextureBind(TextureView textureView, View viewabilityLayer, VideoAd videoAd);
        void onOptInClick();
        void closePrestitial();
        void onClickThrough(View view, AdData ad);
        void onImageLoadComplete(AdData ad, View imageView);
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
