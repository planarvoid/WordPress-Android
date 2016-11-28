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

public class PlanChoicePresenterTest extends AndroidUnitTest {

    private static final AvailableWebProducts BOTH_PLANS = new AvailableWebProducts(Arrays.asList(TestProduct.midTier(), TestProduct.highTier()));

    @Mock PlanChoiceView view;

    private AppCompatActivity activity = activity();
    private PlanChoicePresenter presenter;

    @Before
    public void setUp() {
        activity.setIntent(new Intent().putExtra(PlanChoiceActivity.AVAILABLE_PRODUCTS, BOTH_PLANS));
        presenter = new PlanChoicePresenter(view);
    }

    @Test
    public void displaysProductsFromIntent() {
        presenter.onCreate(activity, null);

        verify(view).displayChoices(BOTH_PLANS);
    }

    @Test
    public void purchaseMidTierPassesProductInfoToCheckout() {
        presenter.onCreate(activity, null);

        presenter.onPurchaseMidTier();

        Assertions.assertThat(activity)
                .nextStartedIntent()
                .containsExtra(WebCheckoutPresenter.PRODUCT_INFO, BOTH_PLANS.midTier().get())
                .opensActivity(WebCheckoutActivity.class);
    }

    @Test
    public void purchaseHighTierPassesProductInfoToCheckout() {
        presenter.onCreate(activity, null);

        presenter.onPurchaseHighTier();

        Assertions.assertThat(activity)
                .nextStartedIntent()
                .containsExtra(WebCheckoutPresenter.PRODUCT_INFO, BOTH_PLANS.highTier().get())
                .opensActivity(WebCheckoutActivity.class);
    }

}
