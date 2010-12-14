package com.soundcloud.utils.AsyncRequest;

import org.apache.http.HttpResponse;

public class CallbackWrapper implements Runnable {
 
	private ResponseListener callbackActivity;
	private HttpResponse response;
 
	public CallbackWrapper(ResponseListener callbackActivity) {
		this.callbackActivity = callbackActivity;
	}
 
	public void run() {
		callbackActivity.onResponseReceived(response);
	}
 
	public void setResponse(HttpResponse response) {
		this.response = response;
	}
 
}