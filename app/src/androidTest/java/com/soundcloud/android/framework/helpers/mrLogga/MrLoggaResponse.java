package com.soundcloud.android.framework.helpers.mrlogga;

class MrLoggaResponse {
    String responseBody;
    boolean success;

    MrLoggaResponse(boolean success, String responseBody) {
        this.success = success;
        this.responseBody = responseBody;
    }
}