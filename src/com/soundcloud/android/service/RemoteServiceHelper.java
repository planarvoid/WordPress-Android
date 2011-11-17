package com.soundcloud.android.service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.widget.Toast;
import com.soundcloud.android.utils.NetworkConnectivityListener;

public class RemoteServiceHelper {

	static final String LOG_TAG = RemoteServiceHelper.class.getSimpleName();
    static final int CONNECTIVITY_MSG = 0;

	private Context mContext;
	private static RemoteServiceHelper mServiceHelper;
    private boolean mForceOnline;
    private final NetworkConnectivityListener mConnectivityListener;
    private Handler mConnHandler;


    public static RemoteServiceHelper getInstance(Context context) {
		if (mServiceHelper == null)
			mServiceHelper = new RemoteServiceHelper(context);
		// to update the context each time a service helper it's called
		mServiceHelper.setContext(context);
		return mServiceHelper;
	}

	public void setContext(Context context) {
		mContext = context;
	}

	public RemoteServiceHelper(Context context) {
		mContext = context;

        // setup connectivity listening
        mConnHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case CONNECTIVITY_MSG:
                }
            }
        };

        mConnectivityListener = new NetworkConnectivityListener()
                .registerHandler(mConnHandler, CONNECTIVITY_MSG)
                .startListening(context);
	}

	public boolean startService(String action) {
		return startService(action, null);
	}

	public boolean startService(String action, Bundle extras) {
		if (isConnected()) {
			Intent intent = null;
			if (action.equals(RemoteService.Actions.SYNC_FAVORITES)) {
				intent = new Intent(mContext, RemoteService.class);
				intent.putExtra("action", action);

			}
			mContext.startService(intent);
			return true;
		} else {
			Toast toast = new Toast(mContext);
			toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
			toast.setDuration(Toast.LENGTH_LONG);
			toast.setText("No Internet Available");
			toast.show();
			return false;
		}
	}

    private boolean isConnected() {
        return mForceOnline || (mConnectivityListener != null && mConnectivityListener.isConnected());
    }

}
