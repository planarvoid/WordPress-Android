package com.soundcloud.android.payments.googleplay;

import static com.soundcloud.android.payments.googleplay.BillingUtil.REQUEST_PRODUCT_DETAILS;
import static com.soundcloud.android.payments.googleplay.BillingUtil.RESPONSE_BUY_INTENT;
import static com.soundcloud.android.payments.googleplay.BillingUtil.RESPONSE_GET_SKU_DETAILS_LIST;
import static com.soundcloud.android.payments.googleplay.BillingUtil.RESPONSE_PURCHASE_DATA_LIST;
import static com.soundcloud.android.payments.googleplay.BillingUtil.RESPONSE_SIGNATURE_LIST;
import static com.soundcloud.android.payments.googleplay.BillingUtil.RESULT_OK;
import static com.soundcloud.android.payments.googleplay.BillingUtil.getResponseCodeFromBundle;
import static com.soundcloud.android.payments.googleplay.BillingUtil.log;
import static com.soundcloud.android.payments.googleplay.BillingUtil.logBillingResponse;

import com.android.vending.billing.IInAppBillingService;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PaymentFailureEvent;
import com.soundcloud.android.payments.ConnectionStatus;
import com.soundcloud.android.payments.ProductDetails;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import org.json.JSONException;

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
    private final EventBusV2 eventBus;

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
    BillingService(DeviceHelper deviceHelper,
                   BillingServiceBinder binder,
                   ResponseProcessor processor,
                   EventBusV2 eventBus) {
        this.deviceHelper = deviceHelper;
        this.binder = binder;
        this.processor = processor;
        this.eventBus = eventBus;
    }

    public Observable<ConnectionStatus> openConnection(Activity bindingActivity) {
        this.bindingActivity = bindingActivity;

        if (binder.canConnect()) {
            binder.connect(bindingActivity, serviceConnection);
        } else {
            log("Billing service is not available on this device");
            connectionSubject.onNext(ConnectionStatus.UNSUPPORTED);
        }
        return connectionSubject.hide();
    }

    public void closeConnection() {
        if (iabService != null) {
            bindingActivity.unbindService(serviceConnection);
            log("Connection closed");
        }
        connectionSubject.onComplete();
        bindingActivity = null;
    }

    public Single<ProductDetails> getDetails(final String id) {
        return Single.create(emitter -> {
            try {
                Bundle response = iabService.getSkuDetails(IAB_VERSION,
                                                           deviceHelper.getPackageName(),
                                                           TYPE_SUBS,
                                                           getSkuBundle(id));
                logBillingResponse("getSkuDetails", getResponseCodeFromBundle(response));

                if (response.containsKey(RESPONSE_GET_SKU_DETAILS_LIST)) {
                    ArrayList<String> responseList = response.getStringArrayList(RESPONSE_GET_SKU_DETAILS_LIST);
                    ProductDetails details = processor.parseProduct(responseList.get(0));
                    emitter.onSuccess(details);
                } else {
                    emitter.onError(new IllegalStateException("No subscription details in IAB service response"));
                }
            } catch (RemoteException | JSONException e) {
                log("Failed to retrieve subscription details");
                emitter.onError(e);
            }
        });
    }

    public Single<SubscriptionStatus> getStatus() {
        return Single.create(emitter -> {
            try {
                Bundle response = iabService.getPurchases(IAB_VERSION,
                                                          deviceHelper.getPackageName(),
                                                          TYPE_SUBS,
                                                          null);
                int responseCode = getResponseCodeFromBundle(response);
                logBillingResponse("getPurchases", responseCode);

                if (responseCode == RESULT_OK) {
                    emitter.onSuccess(extractStatusFromResponse(response));
                } else {
                    emitter.onError(new IllegalStateException("Non-OK subscription status response code from IAB service"));
                }
            } catch (RemoteException | JSONException e) {
                log("Failed to retrieve subscription status");
                emitter.onError(e);
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
            Bundle response = iabService.getBuyIntent(IAB_VERSION,
                                                      deviceHelper.getPackageName(),
                                                      id,
                                                      TYPE_SUBS,
                                                      purchaseUrn);
            int responseCode = getResponseCodeFromBundle(response);
            logBillingResponse("getBuyIntent", responseCode);

            if (responseCode == RESULT_OK) {
                PendingIntent buyIntent = response.getParcelable(RESPONSE_BUY_INTENT);
                bindingActivity.startIntentSenderForResult(buyIntent.getIntentSender(),
                                                           BillingResult.REQUEST_CODE,
                                                           new Intent(),
                                                           NO_FLAGS,
                                                           NO_FLAGS,
                                                           NO_FLAGS);
            }
        } catch (RemoteException | IntentSender.SendIntentException e) {
            log("Failed to send purchase Intent");
            eventBus.publish(EventQueue.TRACKING, PaymentFailureEvent.create("BillingService.startPurchase"));
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
