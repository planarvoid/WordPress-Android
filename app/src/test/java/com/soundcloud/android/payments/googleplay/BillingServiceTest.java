package com.soundcloud.android.payments.googleplay;

import static com.soundcloud.android.payments.googleplay.BillingUtil.RESPONSE_BUY_INTENT;
import static com.soundcloud.android.payments.googleplay.BillingUtil.RESPONSE_CODE;
import static com.soundcloud.android.payments.googleplay.BillingUtil.RESPONSE_GET_SKU_DETAILS_LIST;
import static com.soundcloud.android.payments.googleplay.BillingUtil.RESPONSE_PURCHASE_DATA_LIST;
import static com.soundcloud.android.payments.googleplay.BillingUtil.RESPONSE_SIGNATURE_LIST;
import static com.soundcloud.android.payments.googleplay.BillingUtil.RESULT_ERROR;
import static com.soundcloud.android.payments.googleplay.BillingUtil.RESULT_OK;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.android.vending.billing.IInAppBillingService;
import com.soundcloud.android.payments.ConnectionStatus;
import com.soundcloud.android.payments.ProductDetails;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.Observable;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.ArrayList;

public class BillingServiceTest extends AndroidUnitTest {

    @Mock private DeviceHelper deviceHelper;
    @Mock private ResponseProcessor responseProcessor;
    @Mock private Activity activity;
    @Mock private IBinder binder;
    @Mock private BillingServiceBinder billingBinder;
    @Mock private IInAppBillingService service;
    @Mock private EventBus eventBus;

    @Captor private ArgumentCaptor<ServiceConnection> connectionCaptor;

    private BillingService billingService;

    @Before
    public void setUp() throws Exception {
        billingService = new BillingService(deviceHelper, billingBinder, responseProcessor, eventBus);
        when(billingBinder.bind(binder)).thenReturn(service);
        when(billingBinder.canConnect()).thenReturn(true);
        when(deviceHelper.getPackageName()).thenReturn("com.package");
    }

    @Test
    public void openConnectionBindsBillingService() {
        billingService.openConnection(activity);

        verify(billingBinder).connect(eq(activity), any(ServiceConnection.class));
    }

    @Test
    public void connectionStatusReadyIfConnectedAndSubsSupported() throws RemoteException {
        Observable<ConnectionStatus> result = billingService.openConnection(activity);
        when(service.isBillingSupported(anyInt(), anyString(), anyString())).thenReturn(RESULT_OK);
        onServiceConnected();

        ConnectionStatus status = result.blockingFirst(null);
        assertThat(status.isReady()).isTrue();
    }

    @Test
    public void connectionStatusUnsupportedIfServiceNotAvailable() {
        when(billingBinder.canConnect()).thenReturn(false);

        Observable<ConnectionStatus> result = billingService.openConnection(activity);

        ConnectionStatus status = result.blockingFirst(null);
        assertThat(status.isUnsupported()).isTrue();
    }

    @Test
    public void connectionStatusUnsupportedIfSubsNotAvailable() throws RemoteException {
        Observable<ConnectionStatus> result = billingService.openConnection(activity);
        when(service.isBillingSupported(eq(3), anyString(), anyString())).thenReturn(RESULT_ERROR);
        onServiceConnected();

        ConnectionStatus status = result.blockingFirst(null);
        assertThat(status.isUnsupported()).isTrue();
    }

    @Test
    public void connectionStatusDisconnectedIfConnectionClosed() throws RemoteException {
        Observable<ConnectionStatus> result = billingService.openConnection(activity);
        when(service.isBillingSupported(anyInt(), anyString(), anyString())).thenReturn(RESULT_OK);

        onServiceDisconnected();

        ConnectionStatus status = result.blockingFirst(null);
        assertThat(status.isDisconnected()).isTrue();
    }

    @Test
    public void closeConnectionUnbindsServiceFromActivity() {
        billingService.openConnection(activity);
        onServiceConnected();

        billingService.closeConnection();

        verify(activity).unbindService(any(ServiceConnection.class));
    }

    @Test
    public void getDetailsReturnsProductDataFromBillingService() throws RemoteException, JSONException {
        Bundle bundle = okBundle();
        bundle.putStringArrayList(RESPONSE_GET_SKU_DETAILS_LIST, newArrayList("data"));
        ProductDetails details = new ProductDetails("id", "title", "blah", "$100");
        when(service.getSkuDetails(eq(3), eq("com.package"), eq("subs"), any(Bundle.class))).thenReturn(bundle);
        when(responseProcessor.parseProduct(eq("data"))).thenReturn(details);

        billingService.openConnection(activity);
        onServiceConnected();
        ProductDetails result = billingService.getDetails("id").test().values().get(0);

        assertThat(result).isSameAs(details);
    }

    @Test
    public void getStatusReturnsPayloadIfAlreadySubscribed() throws RemoteException {
        Bundle bundle = okBundle();
        bundle.putStringArrayList(RESPONSE_PURCHASE_DATA_LIST, newArrayList("data"));
        bundle.putStringArrayList(RESPONSE_SIGNATURE_LIST, newArrayList("signature"));
        when(service.getPurchases(3, "com.package", "subs", null)).thenReturn(bundle);


        billingService.openConnection(activity);
        onServiceConnected();
        SubscriptionStatus status = billingService.getStatus().test().values().get(0);

        assertThat(status.isSubscribed()).isTrue();
        assertThat(status.getPayload()).isEqualTo(new Payload("data", "signature"));
    }

    @Test
    public void getStatusReturnsCorrectStatusIfNotSubscribed() throws RemoteException {
        Bundle bundle = okBundle();
        bundle.putStringArrayList(RESPONSE_PURCHASE_DATA_LIST, new ArrayList<>());
        bundle.putStringArrayList(RESPONSE_SIGNATURE_LIST, new ArrayList<>());
        when(service.getPurchases(3, "com.package", "subs", null)).thenReturn(bundle);

        billingService.openConnection(activity);
        onServiceConnected();
        SubscriptionStatus status = billingService.getStatus().test().values().get(0);

        assertThat(status.isSubscribed()).isFalse();
    }

    @Test
    public void startPurchaseStartsIntentFromBillingService() throws RemoteException, IntentSender.SendIntentException {
        Bundle bundle = okBundle();
        bundle.putParcelable(RESPONSE_BUY_INTENT, PendingIntent.getActivity(activity, 0, new Intent(), 0));
        when(service.getBuyIntent(3, "com.package", "package_id", "subs", "token")).thenReturn(bundle);

        billingService.openConnection(activity);
        onServiceConnected();
        billingService.startPurchase("package_id", "token");

        verify(activity).startIntentSenderForResult(any(IntentSender.class),
                                                    eq(BillingResult.REQUEST_CODE),
                                                    any(Intent.class),
                                                    eq(0),
                                                    eq(0),
                                                    eq(0));
    }

    private void onServiceConnected() {
        verify(billingBinder).connect(eq(activity), connectionCaptor.capture());
        ServiceConnection connection = connectionCaptor.getValue();
        connection.onServiceConnected(ComponentName.unflattenFromString(""), binder);
    }

    private void onServiceDisconnected() {
        verify(billingBinder).connect(eq(activity), connectionCaptor.capture());
        ServiceConnection connection = connectionCaptor.getValue();
        connection.onServiceDisconnected(ComponentName.unflattenFromString(""));
    }

    private Bundle okBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(RESPONSE_CODE, RESULT_OK);
        return bundle;
    }

}
