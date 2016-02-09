package com.soundcloud.android.payments;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

public class WebCheckoutPresenterTest extends AndroidUnitTest {

    @Mock private WebCheckoutView view;
    @Mock private AccountOperations accountOperations;
    @Mock private Navigator navigator;

    private AppCompatActivity activity = new AppCompatActivity();

    private WebCheckoutPresenter presenter;

    @Before
    public void setUp() throws Exception {
        when(accountOperations.getSoundCloudToken()).thenReturn(Token.EMPTY);
        WebProduct product = WebProduct.create("high_tier", "some:product:123", "$2", 30, "start", "expiry");
        activity.setIntent(new Intent().putExtra(WebConversionPresenter.PRODUCT_INFO, product));

        presenter = new WebCheckoutPresenter(view, accountOperations, navigator, new TestEventBus());
    }

    @Test
    public void loadsFormOnCreate() {
        presenter.onCreate(activity, null);

        verify(view).loadUrl(WebCheckoutInterface.PAYMENT_FORM_URL);
    }

    @Test
    public void loadsFormOnRetry() {
        presenter.onCreate(activity, null);

        presenter.onRetry();

        verify(view, times(2)).loadUrl(WebCheckoutInterface.PAYMENT_FORM_URL);
    }

    @Test
    public void showsWebViewWhenFormIsLoaded() {
        presenter.onCreate(activity, null);

        presenter.onLoad();

        verify(view).setLoading(false);
    }

    @Test
    public void successfulPaymentTriggersAccountUpgrade() {
        presenter.onCreate(activity, null);

        presenter.onPaymentSuccess();

        verify(navigator).restartForAccountUpgrade(eq(activity));
    }

}
