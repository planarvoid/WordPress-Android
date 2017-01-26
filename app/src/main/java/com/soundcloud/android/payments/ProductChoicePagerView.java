package com.soundcloud.android.payments;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.view.pageindicator.CirclePageIndicator;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;

import javax.inject.Inject;

class ProductChoicePagerView extends ProductChoiceView implements ViewPager.OnPageChangeListener {

    private final ProductChoiceAdapter productChoiceAdapter;
    private final ProductInfoFormatter formatter;

    private Listener listener;

    @BindView(R.id.buy) Button buyButton;
    @BindView(R.id.product_choice_pager) ViewPager pager;
    @BindView(R.id.page_indicator) CirclePageIndicator indicator;

    @Inject
    ProductChoicePagerView(ProductChoiceAdapter productChoiceAdapter, ProductInfoFormatter formatter) {
        this.productChoiceAdapter = productChoiceAdapter;
        this.formatter = formatter;
    }

    @Override
    void setupContent(View view, AvailableWebProducts products, Listener listener) {
        ButterKnife.bind(this, view);
        this.listener = listener;
        pager.setAdapter(productChoiceAdapter);
        pager.addOnPageChangeListener(this);
        indicator.setViewPager(pager);
        displayProducts(products);
    }

    private void displayProducts(AvailableWebProducts products) {
        productChoiceAdapter.setProducts(products);
        configureBuyButton(productChoiceAdapter.getProduct(0));
    }

    @Override
    public void onPageSelected(int position) {
        configureBuyButton(productChoiceAdapter.getProduct(position));
    }

    private void configureBuyButton(WebProduct product) {
        buyButton.setText(formatter.configuredBuyButton(product));
        buyButton.setOnClickListener(v -> listener.onPurchaseProduct(product));
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageScrollStateChanged(int state) {}

}
