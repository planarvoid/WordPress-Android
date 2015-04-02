package com.soundcloud.android.framework.helpers.mrLogga;

class MrLoggaResponse {
    String responseBody;
    boolean success;

    MrLoggaResponse(boolean success, String responseBody) {
        this.success = success;
        this.responseBody = responseBody;
    }
}