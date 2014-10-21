package com.soundcloud.android.payments.googleplay;

import static com.soundcloud.android.payments.googleplay.PlayBillingUtil.log;
import static com.soundcloud.android.payments.googleplay.PlayBillingUtil.logBillingResponse;

import com.android.vending.billing.IInAppBillingService;
import com.google.common.collect.Lists;
import com.soundcloud.android.payments.ConnectionStatus;
import com.soundcloud.android.payments.ProductDetails;
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

public class PlayBillingService {

    private static final int NO_FLAGS = 0;
    private static final int IAB_VERSION = 3;
    private static final String PRODUCT_TYPE = "subs";

    private final DeviceHelper deviceHelper;
    private final BillingServiceBinder binder;
    private final PlayResponseProcessor processor;

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
    PlayBillingService(DeviceHelper deviceHelper, BillingServiceBinder binder, PlayResponseProcessor processor) {
        this.deviceHelper = deviceHelper;
        this.binder = binder;
        this.processor = processor;
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
                    Bundle response = iabService.getSkuDetails(IAB_VERSION, deviceHelper.getPackageName(), PRODUCT_TYPE, getSkuBundle(id));
                    logBillingResponse("getSkuDetails", PlayBillingUtil.getResponseCodeFromBundle(response));

                    if (response.containsKey(PlayBillingUtil.RESPONSE_GET_SKU_DETAILS_LIST)) {
                        ArrayList<String> responseList = response.getStringArrayList(PlayBillingUtil.RESPONSE_GET_SKU_DETAILS_LIST);
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

    public void startPurchase(final String id, final String purchaseUrn) {
        try {
            Bundle response = iabService.getBuyIntent(3, deviceHelper.getPackageName(), id, PRODUCT_TYPE, purchaseUrn);
            int responseCode = PlayBillingUtil.getResponseCodeFromBundle(response);
            logBillingResponse("getBuyIntent", responseCode);
            if (responseCode == PlayBillingUtil.RESULT_OK) {
                PendingIntent buyIntent = response.getParcelable(PlayBillingUtil.RESPONSE_BUY_INTENT);
                bindingActivity.startIntentSenderForResult(buyIntent.getIntentSender(),
                        PlayBillingResult.REQUEST_CODE, new Intent(), NO_FLAGS, NO_FLAGS, NO_FLAGS);
            }
        } catch (RemoteException | IntentSender.SendIntentException e) {
            log("Failed to send purchase Intent");
            ErrorUtils.handleSilentException(e);
        }
    }

    private Bundle getSkuBundle(String sku) {
        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList(PlayBillingUtil.REQUEST_PRODUCT_DETAILS, Lists.newArrayList(sku));
        return querySkus;
    }

    private boolean isSubsSupported() {
        try {
            int responseCode = iabService.isBillingSupported(IAB_VERSION, deviceHelper.getPackageName(), PRODUCT_TYPE);
            logBillingResponse("isSubsSupported", responseCode);
            return responseCode == PlayBillingUtil.RESULT_OK;
        } catch (RemoteException e) {
            return false;
        }
    }

}
