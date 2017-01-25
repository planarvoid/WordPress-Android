package com.soundcloud.android.payments;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.view.pageindicator.CirclePageIndicator;

import android.content.res.Resources;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import javax.inject.Inject;

class ProductChoiceView implements ViewPager.OnPageChangeListener {

    private final Resources resources;
    private final ProductChoiceAdapter productChoiceAdapter;
    private final ProductInfoFormatter formatter;

    private Listener listener;

    @BindView(R.id.buy) Button buyButton;
    @BindView(R.id.product_choice_pager) ViewPager pager;
    @BindView(R.id.page_indicator) CirclePageIndicator indicator;

    interface Listener {
        void onPurchaseProduct(WebProduct product);
    }

    @Inject
    ProductChoiceView(Resources resources, ProductChoiceAdapter productChoiceAdapter, ProductInfoFormatter formatter) {
        this.resources = resources;
        this.productChoiceAdapter = productChoiceAdapter;
        this.formatter = formatter;
    }

    void setupContentView(AppCompatActivity activity, Listener listener) {
        ButterKnife.bind(this, activity.findViewById(android.R.id.content));
        this.listener = listener;
    }

    void displayOptions(AvailableWebProducts products) {
        productChoiceAdapter.setProducts(products);
        pager.setAdapter(productChoiceAdapter);
        pager.addOnPageChangeListener(this);
        indicator.setViewPager(pager);
        configureBuyButton(productChoiceAdapter.getProduct(0));
    }

    @Override
    public void onPageSelected(int position) {
        configureBuyButton(productChoiceAdapter.getProduct(position));
    }

    private void configureBuyButton(WebProduct product) {
        buyButton.setText(product.hasPromo()
                          ? resources.getString(R.string.conversion_buy_promo)
                          : formatter.buyButton(product.getTrialDays()));
        buyButton.setOnClickListener(v -> listener.onPurchaseProduct(product));
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageScrollStateChanged(int state) {}

}
