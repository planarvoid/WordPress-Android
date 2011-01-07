package com.soundcloud.utils.AsyncRequest;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Handler;
 
public class AsynchronousSender extends Thread {
 
	private static final DefaultHttpClient httpClient =
		new DefaultHttpClient();
 
	private HttpUriRequest request;
	private Handler handler;
	private CallbackWrapper wrapper;
 
	protected AsynchronousSender(HttpUriRequest request,
			Handler handler, CallbackWrapper wrapper) {
		this.request = request;
		this.handler = handler;
		this.wrapper = wrapper;
	}
 
	public void run() {
		try {
			final HttpResponse response;
			synchronized (httpClient) {
				response = getClient().execute(request);
			}
			// process response
			wrapper.setResponse(response);
			handler.post(wrapper);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
 
	private HttpClient getClient() {
		return httpClient;
	}
 
}