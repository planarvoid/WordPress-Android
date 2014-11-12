package com.soundcloud.android.payments;

import android.app.Activity;
import android.content.Intent;

public final class BillingResponses {

    private static final int REQUEST_CODE = 1001;

    private static final String SUCCESS_DATA = "{\"orderId\":\"12999763169054705758.1343597682978365\",\"packageName\":\"com.soundcloud.android\",\"productId\":\"android_test_product\",\"purchaseTime\":1414161345678,\"purchaseState\":0,\"developerPayload\":\"soundcloud:payments:orders:e72661985b8911e49a4200e081c198e9\",\"purchaseToken\":\"objhhcjhlomeghckclmnmnfe.AO-J1OxNatDmZqPUrXFbudWE4lIDGUxeuCU_nDxKY2FconuT_ikiUIQvSFi5Q1FLhUp2fvtu9P5ahPHI2V-TCb6Rg74Oo-htz0CAUSme96paccFu1IepnDYmY7Apk-30Wl-800DTA6v9\"}";
    private static final String SUCCESS_SIGNATURE = "DUJnUOpxr3XemvymtNxuHa1XwzaG2Iyg6SPH62DmJz2BBvDrgQXDxTxfdLEQrkjssJjXhYLwwijPrzx+pic6QdzrRUsD0aC0Aphjpj0bg/3lgA4J5PBhQyDJ8ks00AgRuLfQyZe0MOGoBjyvW62Bnarx+9kh1r36bMEmpAW7LBVTnf0PRbGqC4HyLzd/vwpPP+QVZwep6W2NWaNMaDPJnnqtpQdTIEWNJv2ZAet0WbJu0GIwe+cg7Z5ZUD0RMjAqUNqPm6vFGGQVG/IqmAb53ZeEOxEN6nC0zt17tDC0kmhyqcsf4OIy/1mJFI2pR8PqNcY93guzve3NtPVa7wiTqQ==";

    private BillingResponses() {}

    public static void ok(Activity activity) {
        Intent billingSuccess = new Intent();
        billingSuccess.putExtra("RESPONSE_CODE", 0);
        billingSuccess.putExtra("INAPP_PURCHASE_DATA", SUCCESS_DATA);
        billingSuccess.putExtra("INAPP_DATA_SIGNATURE", SUCCESS_SIGNATURE);
        insertResult(activity, Activity.RESULT_OK, billingSuccess);
    }

    public static void cancel(Activity activity) {
        insertResult(activity, Activity.RESULT_CANCELED, null);
    }

    private static void insertResult(final Activity activity, final int responseCode, final Intent data) {
        final SubscribeActivity subscribe = (SubscribeActivity) activity;
        subscribe.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                subscribe.onActivityResult(REQUEST_CODE, responseCode, data);
            }
        });
    }

}
