package com.soundcloud.android.ads;

import static com.soundcloud.android.ads.PrestitialAdapter.PrestitialPage;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;

import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

class SponsoredSessionCardView extends PrestitialView {

    private final ImageOperations imageOperations;
    private final Resources resources;

    @BindView(R.id.ad_image_view) ImageView imageView;
    @BindView(R.id.opt_in_text) TextView descriptionView;
    @BindView(R.id.btn_left) TextView optionOneView;
    @BindView(R.id.btn_right) TextView optionTwoView;

    @Inject
    SponsoredSessionCardView(ImageOperations imageOperations, Resources resources) {
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    public void setupContentView(View view,
                                 SponsoredSessionAd ad,
                                 Listener listener,
                                 PrestitialPage prestitialPage) {
        ButterKnife.bind(this, view);

        imageOperations.displayAdImage(ad.adUrn(), ad.optInCard().imageUrl(), imageView, new PrestitialImageCompanionListener(ad, listener));

        optionOneView.setText(prestitialPage.optionOne);
        optionOneView.setOnClickListener(buttonView-> listener.onOptionOneClick(prestitialPage, ad, buttonView.getContext()));
        optionTwoView.setText(prestitialPage.optionTwo);
        optionTwoView.setOnClickListener(ignored -> listener.onOptionTwoClick(prestitialPage, ad));

        descriptionView.setText(descriptionText(prestitialPage, ad));
    }

    private String descriptionText(PrestitialPage page, SponsoredSessionAd ad) {
        final String descriptionText = resources.getString(page.description);
        return page == PrestitialPage.OPT_IN_CARD ? String.format(descriptionText, ad.adFreeLength())
                                                  : descriptionText;
    }
}
