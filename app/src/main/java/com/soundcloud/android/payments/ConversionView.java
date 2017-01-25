package com.soundcloud.android.payments;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.images.BackgroundDecoder;
import com.soundcloud.android.view.LoadingButton;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

class ConversionView {

    private static final String RESTRICTIONS_DIALOG_TAG = "product_info";
    private static final int BACKGROUND_ID = R.drawable.conversion_background;

    private final Resources resources;
    private final ProductInfoFormatter formatter;
    private final BackgroundDecoder backgroundDecoder;

    private FragmentManager fragmentManager;

    @BindView(R.id.conversion_background) ImageView background;
    @BindView(R.id.conversion_buy) LoadingButton buyButton;
    @BindView(R.id.conversion_price) TextView priceView;
    @BindView(R.id.conversion_restrictions) TextView restrictionsView;
    @BindView(R.id.conversion_more_products) Button moreButton;

    @Inject
    ConversionView(Resources resources, ProductInfoFormatter formatter, BackgroundDecoder backgroundDecoder) {
        this.resources = resources;
        this.formatter = formatter;
        this.backgroundDecoder = backgroundDecoder;
    }

    interface Listener {
        void onPurchasePrimary();
        void onMoreProducts();
    }

    void setupContentView(AppCompatActivity activity, Listener listener) {
        this.fragmentManager = activity.getSupportFragmentManager();
        ButterKnife.bind(this, activity.findViewById(android.R.id.content));
        loadBackground();
        setupListener(listener);
    }

    private void loadBackground() {
        Observable.fromCallable(() -> backgroundDecoder.decode(BACKGROUND_ID))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BackgroundSubscriber());
    }

    private void setupListener(final Listener listener) {
        View.OnClickListener clickListener = v -> {
            switch (v.getId()) {
                case R.id.conversion_buy:
                    listener.onPurchasePrimary();
                    break;
                case R.id.conversion_more_products:
                    listener.onMoreProducts();
                    break;
                default:
                    throw new IllegalArgumentException("Click on unknown View ID");
            }
        };
        buyButton.setOnClickListener(clickListener);
        moreButton.setOnClickListener(clickListener);
    }

    void showDetails(String price) {
        showPrice(price);
        enableBuyButton();
    }

    void showDetails(String price, int trialDays) {
        showPrice(price);
        showTrialDays(trialDays);
        enableBuyButton();
    }

    private void showPrice(String price) {
        priceView.setText(formatter.monthlyPricing(price));
        priceView.setVisibility(View.VISIBLE);
    }

    void showPromo(String promoPrice, int promoDays, String regularPrice) {
        priceView.setText(formatter.promoPricing(promoDays, promoPrice));
        priceView.setVisibility(View.VISIBLE);
        buyButton.setActionText(resources.getString(R.string.conversion_buy_promo));
        setupPromoRestrictions(formatter.promoDuration(promoDays), promoPrice, regularPrice);
        enableBuyButton();
    }

    private void showTrialDays(final int trialDays) {
        buyButton.setActionText(formatter.buyButton(trialDays));
        setupRestrictions(trialDays);
    }

    private void setupRestrictions(final int trialDays) {
        restrictionsView.setOnClickListener(v -> {
            ConversionRestrictionsDialog dialog = trialDays > 0
                    ? ConversionRestrictionsDialog.createForTrial(trialDays)
                    : ConversionRestrictionsDialog.createForNoTrial();
            dialog.show(fragmentManager, RESTRICTIONS_DIALOG_TAG);
        });
        restrictionsView.setVisibility(View.VISIBLE);
    }

    private void setupPromoRestrictions(final String duration, final String promoPrice, final String regularPrice) {
        restrictionsView.setText(resources.getString(R.string.conversion_restrictions_promo, regularPrice));
        restrictionsView.setOnClickListener(v -> ConversionRestrictionsDialog.createForPromo(duration, promoPrice, regularPrice)
                                                                        .show(fragmentManager, RESTRICTIONS_DIALOG_TAG));
        restrictionsView.setVisibility(View.VISIBLE);
    }

    void enableBuyButton() {
        buyButton.setEnabled(true);
        buyButton.setLoading(false);
    }

    void showLoadingState() {
        buyButton.setEnabled(false);
        buyButton.setLoading(true);
    }

    void showRetryState() {
        buyButton.setEnabled(true);
        buyButton.setRetry();
    }

    void enableMorePlans() {
        moreButton.setVisibility(View.VISIBLE);
    }

    private class BackgroundSubscriber extends DefaultSubscriber<Bitmap> {
        @Override
        public void onNext(Bitmap image) {
            background.setImageBitmap(image);
        }
    }

}
