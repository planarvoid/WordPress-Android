package com.soundcloud.android.payments.googleplay;

import com.soundcloud.android.utils.Log;

import android.content.Intent;
import android.os.Bundle;

/*
 * Codes and utils for InAppBillingService.aidl
 */
final class PlayBillingUtil {

    private static final String TAG = "PlayBilling";

    public static final int ERROR_REMOTE_EXCEPTION = -1;

    public static final int RESULT_OK = 0;
    public static final int RESULT_USER_CANCELED = 1;
    public static final int RESULT_BILLING_UNAVAILABLE = 3;
    public static final int RESULT_ITEM_UNAVAILABLE = 4;
    public static final int RESULT_DEVELOPER_ERROR = 5;
    public static final int RESULT_ERROR = 6;
    public static final int RESULT_ITEM_ALREADY_OWNED = 7;
    public static final int RESULT_ITEM_NOT_OWNED = 8;

    public static final String RESPONSE_CODE = "RESPONSE_CODE";
    public static final String RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST";
    public static final String RESPONSE_BUY_INTENT = "BUY_INTENT";
    public static final String RESPONSE_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
    public static final String RESPONSE_SIGNATURE = "INAPP_DATA_SIGNATURE";
    public static final String RESPONSE_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";
    public static final String RESPONSE_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
    public static final String RESPONSE_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";

    public static final String REQUEST_PRODUCT_DETAILS = "ITEM_ID_LIST";
    public static final String REQUEST_PRODUCT_DETAILS_TYPES = "ITEM_TYPE_LIST";

    /*
     * The InAppBillingService reference implementation states that response codes should be Integers,
     * but there is a bug that causes them to return as Longs in some cases.
     *
     * Also, apparently having no response code means OK. Whaaaat?
     */

    public static int getResponseCodeFromBundle(Bundle bundle) {
        Object code = bundle.get(RESPONSE_CODE);
        return responseCodeFromObject(code);
    }

    public static int getResponseCodeFromIntent(Intent intent) {
        Object code = intent.getExtras().get(RESPONSE_CODE);
        return responseCodeFromObject(code);
    }

    private static int responseCodeFromObject(Object code) {
        if (code == null) { return RESULT_OK; }
        else if (code instanceof Integer) { return (Integer) code; }
        else if (code instanceof Long) { return ((Long) code).intValue(); }
        else { throw new IllegalArgumentException("Invalid type for Google Play billing RESPONSE_CODE"); }
    }

    public static String removeAppName(String productTitle) {
        return productTitle.replaceAll("\\(.*?\\)", "").trim();
    }

    public static void log(String message) {
        Log.d(TAG, message);
    }

    public static void logBillingResponse(String message, int responseCode) {
        switch (responseCode) {
            case PlayBillingUtil.RESULT_OK:
                Log.d(TAG, message + ": OK");
                break;
            case PlayBillingUtil.RESULT_BILLING_UNAVAILABLE:
                Log.e(TAG, message + ": UNAVAILABLE");
                break;
            case PlayBillingUtil.ERROR_REMOTE_EXCEPTION:
                Log.e(TAG, message + ": RemoteException");
                break;
            default:
                Log.e(TAG, message + ": Unknown response code of " + responseCode);
        }
    }

}
