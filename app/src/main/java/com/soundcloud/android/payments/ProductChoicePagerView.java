package com.soundcloud.android.payments;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.view.pageindicator.CirclePageIndicator;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import javax.inject.Inject;

class ProductChoicePagerView extends ProductChoiceView implements ViewPager.OnPageChangeListener {

    private final ProductChoiceAdapter productChoiceAdapter;
    private final ProductInfoFormatter formatter;

    private Listener listener;

    @BindView(R.id.progress_container) View progressContainer;
    @BindView(R.id.buy) Button buyButton;
    @BindView(R.id.product_choice_restrictions) TextView restrictions;
    @BindView(R.id.product_choice_pager) ViewPager pager;
    @BindView(R.id.page_indicator) CirclePageIndicator indicator;

    @Inject
    ProductChoicePagerView(ProductChoiceAdapter productChoiceAdapter, ProductInfoFormatter formatter) {
        this.productChoiceAdapter = productChoiceAdapter;
        this.formatter = formatter;
    }

    @Override
    void showContent(View view, AvailableWebProducts products, Listener listener, Plan initialPlan) {
        ButterKnife.bind(this, view);
        this.listener = listener;
        configureView(products, initialPlan);
    }

    private void configureView(AvailableWebProducts products, Plan plan) {
        productChoiceAdapter.setProducts(products);
        pager.setAdapter(productChoiceAdapter);
        pager.addOnPageChangeListener(this);
        indicator.setViewPager(pager);
        pager.setCurrentItem(Plan.MID_TIER == plan ?
                             ProductChoiceAdapter.PRODUCT_MID_TIER_INDEX :
                             ProductChoiceAdapter.PRODUCT_HIGH_TIER_INDEX);
        configureButtons(productChoiceAdapter.getProduct(pager.getCurrentItem()));
        progressContainer.setVisibility(View.GONE);
    }

    @Override
    public void onPageSelected(int position) {
        configureButtons(productChoiceAdapter.getProduct(position));
    }

    private void configureButtons(WebProduct product) {
        listener.onBuyImpression(product);
        buyButton.setText(formatter.configuredBuyButton(product));
        buyButton.setOnClickListener(v -> listener.onBuyClick(product));
        restrictions.setText(formatter.configuredRestrictionsText(product));
        restrictions.setOnClickListener(v -> listener.onRestrictionsClick(product));
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageScrollStateChanged(int state) {}

}
