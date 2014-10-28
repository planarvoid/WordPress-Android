package com.soundcloud.android.payments.googleplay;

import static com.soundcloud.android.Expect.expect;
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

@RunWith(SoundCloudTestRunner.class)
public class PlayBillingServiceTest {

    @Mock DeviceHelper deviceHelper;
    @Mock PlayResponseProcessor responseProcessor;
    @Mock Activity activity;
    @Mock IBinder iBinder;
    @Mock BillingServiceBinder billingBinder;
    @Mock IInAppBillingService service;

    @Captor ArgumentCaptor<ServiceConnection> connectionCaptor;

    private PlayBillingService billingService;

    @Before
    public void setUp() throws Exception {
        billingService = new PlayBillingService(deviceHelper, billingBinder, responseProcessor);
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
        when(service.isBillingSupported(anyInt(), anyString(), anyString())).thenReturn(PlayBillingUtil.RESULT_OK);
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
        when(service.isBillingSupported(eq(3), anyString(), anyString())).thenReturn(PlayBillingUtil.RESULT_ERROR);
        onServiceConnected();

        ConnectionStatus status = result.toBlocking().firstOrDefault(null);
        expect(status.isUnsupported()).toBeTrue();
    }

    @Test
    public void connectionStatusDisconnectedIfConnectionClosed() throws RemoteException {
        Observable<ConnectionStatus> result = billingService.openConnection(activity);
        when(service.isBillingSupported(anyInt(), anyString(), anyString())).thenReturn(PlayBillingUtil.RESULT_OK);

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
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(PlayBillingUtil.RESPONSE_GET_SKU_DETAILS_LIST, Lists.newArrayList("data"));
        ProductDetails details = new ProductDetails("id", "title", "blah", "$100");
        when(service.getSkuDetails(eq(3), eq("com.package"), eq("subs"), any(Bundle.class))).thenReturn(bundle);
        when(responseProcessor.parseProduct(eq("data"))).thenReturn(details);

        billingService.openConnection(activity);
        onServiceConnected();
        ProductDetails result = billingService.getDetails("id").toBlocking().firstOrDefault(null);

        expect(result).toBe(details);
    }

    @Test
    public void startPurchaseStartsIntentFromBillingService() throws RemoteException, IntentSender.SendIntentException {
        Bundle bundle = new Bundle();
        bundle.putInt(PlayBillingUtil.RESPONSE_CODE, PlayBillingUtil.RESULT_OK);
        bundle.putParcelable(PlayBillingUtil.RESPONSE_BUY_INTENT, PendingIntent.getActivity(activity, 0, new Intent(), 0));
        when(service.getBuyIntent(3, "com.package", "package_id", "subs", "token")).thenReturn(bundle);

        billingService.openConnection(activity);
        onServiceConnected();
        billingService.startPurchase("package_id", "token");

        verify(activity).startIntentSenderForResult(any(IntentSender.class), eq(PlayBillingResult.REQUEST_CODE), any(Intent.class), eq(0), eq(0), eq(0));
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

}