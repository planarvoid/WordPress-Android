package com.soundcloud.android.payments.googleplay;

import static com.soundcloud.android.payments.googleplay.BillingUtil.*;

import com.android.vending.billing.IInAppBillingService;
import com.google.common.collect.Lists;
import com.soundcloud.android.payments.ConnectionStatus;
import com.soundcloud.android.payments.ProductDetails;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.ErrorUtils;
import org.json.JSONException;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.BehaviorSubject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import javax.inject.Inject;
import java.util.ArrayList;

public class BillingService {

    private static final int NO_FLAGS = 0;
    private static final int IAB_VERSION = 3;
    private static final String TYPE_SUBS = "subs";

    private final DeviceHelper deviceHelper;
    private final BillingServiceBinder binder;
    private final ResponseProcessor processor;
    private final FeatureFlags flags;

    private final BehaviorSubject<ConnectionStatus> connectionSubject = BehaviorSubject.create();

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            log("Service connected");
            iabService = binder.bind(service);
            connectionSubject.onNext(isSubsSupported() ? ConnectionStatus.READY : ConnectionStatus.UNSUPPORTED);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            log("Service disconnected");
            iabService = null;
            connectionSubject.onNext(ConnectionStatus.DISCONNECTED);
        }
    };

    private Activity bindingActivity;
    private IInAppBillingService iabService;

    @Inject
    BillingService(DeviceHelper deviceHelper, BillingServiceBinder binder, ResponseProcessor processor, FeatureFlags flags) {
        this.deviceHelper = deviceHelper;
        this.binder = binder;
        this.processor = processor;
        this.flags = flags;
    }

    public Observable<ConnectionStatus> openConnection(Activity bindingActivity) {
        this.bindingActivity = bindingActivity;

        if (binder.canConnect()) {
            binder.connect(bindingActivity, serviceConnection);
        } else {
            log("Billing service is not available on this device");
            connectionSubject.onNext(ConnectionStatus.UNSUPPORTED);
        }
        return connectionSubject.asObservable();
    }

    public void closeConnection() {
        if (iabService != null) {
            bindingActivity.unbindService(serviceConnection);
            log("Connection closed");
        }
        connectionSubject.onCompleted();
        bindingActivity = null;
    }

    public Observable<ProductDetails> getDetails(final String id) {
        return Observable.create(new Observable.OnSubscribe<ProductDetails>() {
            @Override
            public void call(Subscriber<? super ProductDetails> subscriber) {
                try {
                    Bundle response = iabService.getSkuDetails(IAB_VERSION, deviceHelper.getPackageName(), TYPE_SUBS, getSkuBundle(id));
                    logBillingResponse("getSkuDetails", getResponseCodeFromBundle(response));

                    if (response.containsKey(RESPONSE_GET_SKU_DETAILS_LIST)) {
                        ArrayList<String> responseList = response.getStringArrayList(RESPONSE_GET_SKU_DETAILS_LIST);
                        ProductDetails details = processor.parseProduct(responseList.get(0));
                        subscriber.onNext(details);
                    }
                    subscriber.onCompleted();
                } catch (RemoteException | JSONException e) {
                    log("Failed to retrieve subscription details");
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<SubscriptionStatus> getStatus() {
        return Observable.create(new Observable.OnSubscribe<SubscriptionStatus>() {
            @Override
            public void call(Subscriber<? super SubscriptionStatus> subscriber) {
                try {
                    Bundle response = iabService.getPurchases(IAB_VERSION, deviceHelper.getPackageName(), TYPE_SUBS, null);
                    int responseCode = getResponseCodeFromBundle(response);
                    logBillingResponse("getPurchases", responseCode);

                    if (responseCode == RESULT_OK) {
                        subscriber.onNext(extractStatusFromResponse(response));
                    }
                    subscriber.onCompleted();
                } catch (RemoteException | JSONException e) {
                    log("Failed to retrieve subscription status");
                    subscriber.onError(e);
                }
            }
        });
    }

    private SubscriptionStatus extractStatusFromResponse(Bundle response) throws JSONException {
        ArrayList<String> data = response.getStringArrayList(RESPONSE_PURCHASE_DATA_LIST);
        ArrayList<String> signatures = response.getStringArrayList(RESPONSE_SIGNATURE_LIST);
        if (data.isEmpty()) {
            return SubscriptionStatus.notSubscribed();
        } else {
            Payload payload = new Payload(data.get(0), signatures.get(0));
            String token = processor.extractToken(data.get(0));
            return SubscriptionStatus.subscribed(token, payload);
        }
    }

    public void startPurchase(final String id, final String purchaseUrn) {
        try {
            Bundle response = iabService.getBuyIntent(IAB_VERSION, deviceHelper.getPackageName(), id, TYPE_SUBS, purchaseUrn);
            int responseCode = getResponseCodeFromBundle(response);
            logBillingResponse("getBuyIntent", responseCode);

            if (responseCode == RESULT_OK && flags.isDisabled(Flag.PAYMENTS_TEST)) {
                PendingIntent buyIntent = response.getParcelable(RESPONSE_BUY_INTENT);
                bindingActivity.startIntentSenderForResult(buyIntent.getIntentSender(),
                        BillingResult.REQUEST_CODE, new Intent(), NO_FLAGS, NO_FLAGS, NO_FLAGS);
            }
        } catch (RemoteException | IntentSender.SendIntentException e) {
            log("Failed to send purchase Intent");
            ErrorUtils.handleSilentException(e);
        }
    }

    private Bundle getSkuBundle(String sku) {
        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList(REQUEST_PRODUCT_DETAILS, Lists.newArrayList(sku));
        return querySkus;
    }

    private boolean isSubsSupported() {
        try {
            int responseCode = iabService.isBillingSupported(IAB_VERSION, deviceHelper.getPackageName(), TYPE_SUBS);
            logBillingResponse("isSubsSupported", responseCode);
            return responseCode == RESULT_OK;
        } catch (RemoteException e) {
            return false;
        }
    }

}
