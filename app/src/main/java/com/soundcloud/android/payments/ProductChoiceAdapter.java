package com.soundcloud.android.payments;

import butterknife.ButterKnife;
import com.soundcloud.android.R;

import android.support.annotation.LayoutRes;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

class ProductChoiceAdapter extends PagerAdapter {

    private static final int NUM_PRODUCTS = 2;

    private final ProductInfoFormatter formatter;

    private AvailableWebProducts products;

    @Inject
    ProductChoiceAdapter(ProductInfoFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        switch (position) {
            case 0:
                return bindView(container, R.layout.product_page_mid, getProduct(0));
            case 1:
                return bindView(container, R.layout.product_page_high, getProduct(1));
            default:
                throw new IllegalStateException("Unexpected index in " + ProductChoiceAdapter.class.getSimpleName());
        }
    }

    private ViewGroup bindView(ViewGroup parent, @LayoutRes int layout, WebProduct product) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        TextView price = ButterKnife.findById(view, R.id.product_choice_price);
        price.setText(formatter.configuredPrice(product));
        parent.addView(view);
        return view;
    }

    @Override
    public int getCount() {
        return NUM_PRODUCTS;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    public void setProducts(AvailableWebProducts products) {
        this.products = products;
    }

    WebProduct getProduct(int position) {
        switch (position) {
            case 0:
                return products.midTier().get();
            case 1:
                return products.highTier().get();
            default:
                throw new IllegalStateException("Unexpected index in " + ProductChoiceAdapter.class.getSimpleName());
        }
    }

}
