package com.soundcloud.android.payments;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import java.util.Arrays;

public class ProductChoicePresenterTest extends AndroidUnitTest {

    private static final AvailableWebProducts BOTH_PLANS = new AvailableWebProducts(Arrays.asList(TestProduct.midTier(), TestProduct.highTier()));

    @Mock ProductChoiceView view;

    private AppCompatActivity activity = activity();
    private ProductChoicePresenter presenter;

    @Before
    public void setUp() {
        activity.setIntent(new Intent().putExtra(ProductChoiceActivity.AVAILABLE_PRODUCTS, BOTH_PLANS));
        presenter = new ProductChoicePresenter(view);
    }

    @Test
    public void displaysProductsFromIntent() {
        presenter.onCreate(activity, null);

        verify(view).displayOptions(BOTH_PLANS);
    }

    @Test
    public void purchasePassesProductInfoToCheckout() {
        WebProduct product = BOTH_PLANS.midTier().get();
        presenter.onCreate(activity, null);

        presenter.onPurchaseProduct(product);

        Assertions.assertThat(activity)
                .nextStartedIntent()
                .containsExtra(WebCheckoutPresenter.PRODUCT_INFO, product)
                .opensActivity(WebCheckoutActivity.class);
    }

}
