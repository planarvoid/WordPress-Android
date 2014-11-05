package com.soundcloud.android.payments.googleplay;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.payments.googleplay.BillingUtil.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.android.vending.billing.IInAppBillingService;
import com.google.common.collect.Lists;
import com.soundcloud.android.payments.ConnectionStatus;
import com.soundcloud.android.payments.ProductDetails;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.DeviceHelper;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

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

@RunWith(SoundCloudTestRunner.class)
public class BillingServiceTest {

    @Mock private DeviceHelper deviceHelper;
    @Mock private ResponseProcessor responseProcessor;
    @Mock private Activity activity;
    @Mock private IBinder iBinder;
    @Mock private BillingServiceBinder billingBinder;
    @Mock private IInAppBillingService service;

    @Captor private ArgumentCaptor<ServiceConnection> connectionCaptor;

    private BillingService billingService;

    @Before
    public void setUp() throws Exception {
        billingService = new BillingService(deviceHelper, billingBinder, responseProcessor);
        when(billingBinder.bind(iBinder)).thenReturn(service);
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

        ConnectionStatus status = result.toBlocking().firstOrDefault(null);
        expect(status.isReady()).toBeTrue();
    }

    @Test
    public void connectionStatusUnsupportedIfServiceNotAvailable() {
        when(billingBinder.canConnect()).thenReturn(false);

        Observable<ConnectionStatus> result = billingService.openConnection(activity);

        ConnectionStatus status = result.toBlocking().firstOrDefault(null);
        expect(status.isUnsupported()).toBeTrue();
    }

    @Test
    public void connectionStatusUnsupportedIfSubsNotAvailable() throws RemoteException {
        Observable<ConnectionStatus> result = billingService.openConnection(activity);
        when(service.isBillingSupported(eq(3), anyString(), anyString())).thenReturn(RESULT_ERROR);
        onServiceConnected();

        ConnectionStatus status = result.toBlocking().firstOrDefault(null);
        expect(status.isUnsupported()).toBeTrue();
    }

    @Test
    public void connectionStatusDisconnectedIfConnectionClosed() throws RemoteException {
        Observable<ConnectionStatus> result = billingService.openConnection(activity);
        when(service.isBillingSupported(anyInt(), anyString(), anyString())).thenReturn(RESULT_OK);

        onServiceDisconnected();

        ConnectionStatus status = result.toBlocking().firstOrDefault(null);
        expect(status.isDisconnected()).toBeTrue();
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
        bundle.putStringArrayList(RESPONSE_GET_SKU_DETAILS_LIST, Lists.newArrayList("data"));
        ProductDetails details = new ProductDetails("id", "title", "blah", "$100");
        when(service.getSkuDetails(eq(3), eq("com.package"), eq("subs"), any(Bundle.class))).thenReturn(bundle);
        when(responseProcessor.parseProduct(eq("data"))).thenReturn(details);

        billingService.openConnection(activity);
        onServiceConnected();
        ProductDetails result = billingService.getDetails("id").toBlocking().firstOrDefault(null);

        expect(result).toBe(details);
    }

    @Test
    public void getStatusReturnsPayloadIfAlreadySubscribed() throws RemoteException {
        Bundle bundle = okBundle();
        bundle.putStringArrayList(RESPONSE_PURCHASE_DATA_LIST, Lists.newArrayList("data"));
        bundle.putStringArrayList(RESPONSE_SIGNATURE_LIST, Lists.newArrayList("signature"));
        when(service.getPurchases(3, "com.package", "subs", null)).thenReturn(bundle);


        billingService.openConnection(activity);
        onServiceConnected();
        SubscriptionStatus status = billingService.getStatus().toBlocking().firstOrDefault(null);

        expect(status.isSubscribed()).toBeTrue();
        expect(status.getPayload()).toEqual(new Payload("data", "signature"));
    }

    @Test
    public void getStatusReturnsCorrectStatusIfNotSubscribed() throws RemoteException {
        Bundle bundle = okBundle();
        bundle.putStringArrayList(RESPONSE_PURCHASE_DATA_LIST, new ArrayList<String>());
        bundle.putStringArrayList(RESPONSE_SIGNATURE_LIST, new ArrayList<String>());
        when(service.getPurchases(3, "com.package", "subs", null)).thenReturn(bundle);

        billingService.openConnection(activity);
        onServiceConnected();
        SubscriptionStatus status = billingService.getStatus().toBlocking().firstOrDefault(null);

        expect(status.isSubscribed()).toBeFalse();
    }

    @Test
    public void startPurchaseStartsIntentFromBillingService() throws RemoteException, IntentSender.SendIntentException {
        Bundle bundle = okBundle();
        bundle.putParcelable(RESPONSE_BUY_INTENT, PendingIntent.getActivity(activity, 0, new Intent(), 0));
        when(service.getBuyIntent(3, "com.package", "package_id", "subs", "token")).thenReturn(bundle);

        billingService.openConnection(activity);
        onServiceConnected();
        billingService.startPurchase("package_id", "token");

        verify(activity).startIntentSenderForResult(any(IntentSender.class), eq(BillingResult.REQUEST_CODE), any(Intent.class), eq(0), eq(0), eq(0));
    }

    private void onServiceConnected() {
        verify(billingBinder).connect(eq(activity), connectionCaptor.capture());
        ServiceConnection connection = connectionCaptor.getValue();
        connection.onServiceConnected(ComponentName.unflattenFromString(""), iBinder);
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