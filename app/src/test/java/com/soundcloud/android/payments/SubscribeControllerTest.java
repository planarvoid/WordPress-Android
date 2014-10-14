package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

@RunWith(SoundCloudTestRunner.class)
public class SubscribeControllerTest {

    @Mock Activity activity;
    @Mock PaymentOperations paymentOperations;

    private SubscribeController controller;
    private View contentView;

    @Before
    public void setUp() throws Exception {
        controller = new SubscribeController(paymentOperations);
        contentView = LayoutInflater.from(Robolectric.application).inflate(R.layout.payments_activity, null, false);
        when(activity.findViewById(anyInt())).thenReturn(contentView);
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.DISCONNECTED));
    }

    @Test
    public void onCreateSetsActivityContentView() {
        controller.onCreate(activity);
        verify(activity).setContentView(R.layout.payments_activity);
    }

    @Test
    public void onCreateConnectsPaymentOperations() {
        controller.onCreate(activity);
        verify(paymentOperations).connect(activity);
    }

    @Test
    public void onDestroyDisconnectsPaymentOperations() {
        controller.onDestroy();
        verify(paymentOperations).disconnect();
    }

    @Test
    public void doesNotQueryProductDetailsIfBillingIsNotSupported() {
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.UNSUPPORTED));

        controller.onCreate(activity);

        verify(paymentOperations, never()).queryProductDetails();
    }

    @Test
    public void queriesProductDetailsWhenBillingServiceIsConnected() {
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryProductDetails()).thenReturn(Observable.<ProductDetails>empty());

        controller.onCreate(activity);

        verify(paymentOperations).queryProductDetails();
    }

    @Test
    public void displaysProductDetailsWhenPaymentConnectionStatusIsReady() {
        ProductDetails details = new ProductDetails("id", "product title", "description", "$100");
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryProductDetails()).thenReturn(Observable.just(details));

        controller.onCreate(activity);

        expect(getText(R.id.subscribe_title)).toEqual(details.title);
        expect(getText(R.id.subscribe_description)).toEqual(details.description);
        expect(getText(R.id.subscribe_price)).toEqual(details.price);
    }

    private String getText(int id) {
        return ((TextView) contentView.findViewById(id)).getText().toString();
    }

}