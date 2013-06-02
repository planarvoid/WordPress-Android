package com.soundcloud.android.api;


import java.util.Map;

public interface APIRequest {
    WebServiceEndPoint getEndPoint();
    String getMethod();
    Map<String, String> getQueryParameters();

}
