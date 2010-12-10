package com.soundcloud.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;

import android.os.Handler;
import android.util.Log;

public class HTTPThread extends Thread {
	public static final int STATUS_PENDING = 0;
	public static final int STATUS_RUNNING = 1;
	public static final int STATUS_FINISHED = 2;

	private boolean mError = false;
	private Exception mException = null;
	private String mUrl;
	private String mLocal;
	private int mStatus = STATUS_PENDING;
	private SoftReference<Handler> mHandler;

	public HTTPThread(String url, String local, Handler handler) {
		Log.e("[THREAD]", "New HTTP Thread " + url);
		mUrl = url;
		mLocal = local;
		mHandler = new SoftReference<Handler>(handler);
	}

	@Override
	public void start() {
		if (getStatus() == STATUS_PENDING) {
			synchronized (this) {
				mStatus = STATUS_RUNNING;
			}
			super.start();
		}
	}

	@Override
	public void run() {
		try {
			Log.e("[THREAD]", "New Request To " + mUrl);
			URL request = new URL(mUrl);
			InputStream is = (InputStream) request.getContent();
			FileOutputStream fos = new FileOutputStream(mLocal);
			try {
				byte[] buffer = new byte[4096];
				int l;
				while ((l = is.read(buffer)) != -1) {
					fos.write(buffer, 0, l);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (is != null) is.close();
				if (fos != null) fos.flush();
				if (fos != null) fos.close();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		synchronized (this) {
			mStatus = STATUS_FINISHED;
		}
		Handler handler = getHandler();
		if (handler != null) {
			handler.sendEmptyMessage(STATUS_FINISHED);
		}
	}

	public int getStatus() {
		synchronized (this) {
			return mStatus;
		}
	}

	public boolean hasError() {
		return mError;
	}

	public Exception getException() {
		return mException;
	}

	public void setHandler(Handler handler) {
		mHandler = new SoftReference<Handler>(handler);
	}

	public Handler getHandler() {
		if (mHandler != null) {
			return mHandler.get();
		}
		return null;
	}

	@Override
	public long getId() {
		return mUrl.hashCode();
	}
}
