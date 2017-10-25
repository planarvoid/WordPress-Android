package com.soundcloud.android.ads;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.java.optional.Optional;
import io.reactivex.disposables.CompositeDisposable;

import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

class VisualPrestitialView extends PrestitialView {

    private final ImageOperations imageOperations;
    @SuppressLint("sc.MissingCompositeDisposableRecycle")
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @BindView(R.id.ad_image_view) ImageView imageView;
    @BindView(R.id.btn_continue) View continueButton;

    @Inject
    VisualPrestitialView(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    public void setupContentView(AppCompatActivity activity, VisualPrestitialAd ad, Listener listener) {
        ButterKnife.bind(this, activity);

        compositeDisposable.add(imageOperations.displayAdImage(ad.adUrn(), ad.imageUrl(), imageView)
                                               .subscribeWith(PrestitialView.observer(ad, listener, Optional.absent())));

        continueButton.setOnClickListener(btnView -> listener.onContinueClick());
        imageView.setOnClickListener(imageView -> listener.onImageClick(imageView.getContext(), ad, Optional.absent()));
    }
}
