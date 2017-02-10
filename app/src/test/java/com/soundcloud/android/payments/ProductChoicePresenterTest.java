package com.soundcloud.android.payments;

import static com.soundcloud.android.testsupport.InjectionSupport.lazyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.Arrays;

public class ProductChoicePresenterTest extends AndroidUnitTest {

    private static final AvailableWebProducts BOTH_PLANS = new AvailableWebProducts(Arrays.asList(TestProduct.midTier(), TestProduct.highTier()));

    @Mock ProductChoicePagerView pagerView;
    @Mock ProductChoiceScrollView scrollView;
    @Mock ProductInfoFormatter formatter;

    private AppCompatActivity activity = activity();
    private TestEventBus eventBus = new TestEventBus();
    private ProductChoicePresenter presenter;

    @Before
    public void setUp() {
        activity.setIntent(new Intent().putExtra(ProductChoiceActivity.AVAILABLE_PRODUCTS, BOTH_PLANS));
        presenter = new ProductChoicePresenter(lazyOf(pagerView), lazyOf(scrollView), formatter, eventBus);
    }

    @Test
    public void displaysProductsFromIntentWithPagerView() {
        activity.setContentView(R.layout.product_choice_activity);

        presenter.onCreate(activity, null);

        verify(pagerView).setupContent(any(View.class), eq(BOTH_PLANS), eq(presenter));
    }

    @Test
    public void displaysProductsFromIntentWithScrollView() {
        // Not setting up content view to test other branch - in reality landscape config has no pager

        presenter.onCreate(activity, null);

        verify(scrollView).setupContent(any(View.class), eq(BOTH_PLANS), eq(presenter));
    }

    @Test
    public void tracksMidTierBuyImpression() {
        presenter.onCreate(activity, null);

        presenter.onBuyImpression(TestProduct.midTier());

        final UpgradeFunnelEvent event = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.CHOOSER_BUY_MID_TIER.toString());
    }

    @Test
    public void tracksHighTierBuyImpression() {
        presenter.onCreate(activity, null);

        presenter.onBuyImpression(TestProduct.highTier());

        final UpgradeFunnelEvent event = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(event.impressionObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.CHOOSER_BUY_HIGH_TIER.toString());
    }

    @Test
    public void tracksMidTierBuyClick() {
        presenter.onCreate(activity, null);

        presenter.onBuyClick(TestProduct.midTier());

        final UpgradeFunnelEvent event = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.CHOOSER_BUY_MID_TIER.toString());
    }

    @Test
    public void tracksHighTierBuyClick() {
        presenter.onCreate(activity, null);

        presenter.onBuyClick(TestProduct.highTier());

        final UpgradeFunnelEvent event = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.CHOOSER_BUY_HIGH_TIER.toString());
    }

    @Test
    public void purchasePassesProductInfoToCheckout() {
        WebProduct product = BOTH_PLANS.midTier().get();
        presenter.onCreate(activity, null);

        presenter.onBuyClick(product);

        com.soundcloud.android.testsupport.Assertions.assertThat(activity)
                .nextStartedIntent()
                .containsExtra(WebCheckoutPresenter.PRODUCT_INFO, product)
                .opensActivity(WebCheckoutActivity.class);
    }

}
