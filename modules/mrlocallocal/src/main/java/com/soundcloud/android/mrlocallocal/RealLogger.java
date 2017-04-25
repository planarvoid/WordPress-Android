package com.soundcloud.android.mrlocallocal;

import android.util.Log;

class RealLogger implements Logger {
	private static final String TAG = "MrLocalLocal";

	@Override
	public void info(String message) {
		Log.w(TAG, message);
	}

	@Override
	public void error(String message) {
		Log.e(TAG, message);
	}
}
