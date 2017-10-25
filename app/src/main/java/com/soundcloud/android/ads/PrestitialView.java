package com.soundcloud.android.ads;

import static com.soundcloud.android.ads.PrestitialAdapter.PrestitialPage;

import com.soundcloud.android.image.LoadingState;
import com.soundcloud.android.rx.observers.LambdaObserver;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.view.TextureView;
import android.view.View;

abstract class PrestitialView {

    public interface Listener {
        void onTogglePlayback();
        void onVideoTextureBind(TextureView textureView, View viewabilityLayer, VideoAd videoAd);

        void onSkipAd();
        void onWhyAdsClicked(Context context);
        void onImageLoadComplete(AdData ad, View imageView, Optional<PrestitialPage> page);
        void onImageClick(Context context, AdData ad, Optional<PrestitialPage> page);
        void onOptionOneClick(PrestitialPage page, SponsoredSessionAd ad, Context context);
        void onOptionTwoClick(PrestitialPage page, SponsoredSessionAd ad);
        void onContinueClick();
    }

    static LambdaObserver<LoadingState> observer(AdData ad, Listener listener, Optional<PrestitialPage> page) {
        return LambdaObserver.onNext(state -> {
            if (state instanceof LoadingState.Complete) {
                listener.onImageLoadComplete(ad, ((LoadingState.Complete) state).getView(), page);
            }
        });
    }

}
