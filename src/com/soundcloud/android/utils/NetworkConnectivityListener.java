package com.soundcloud.android.utils;

import com.soundcloud.android.SoundCloudApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper for a broadcast receiver that provides network connectivity state
 * information, independent of network type (mobile, Wi-Fi, etc.).
 */
public class NetworkConnectivityListener {
    private static final String TAG = "NetworkConnectivityListener";

    private Context mContext;
    private Map<Handler, Integer> mHandlers = new HashMap<Handler, Integer>();

    private State mState;
    private boolean mListening;

    /**
     * Network connectivity information
     */
    private NetworkInfo mNetworkInfo;

    /**
     * In case of a Disconnect, the connectivity manager may have already
     * established, or may be attempting to establish, connectivity with another
     * network. If so, {@code mOtherNetworkInfo} will be non-null.
     */
    private NetworkInfo mOtherNetworkInfo;

    private ConnectivityBroadcastReceiver mReceiver;

    public enum State {
        UNKNOWN,

        /**
         * This state is returned if there is connectivity to any network *
         */
        CONNECTED,
        /**
         * This state is returned if there is no connectivity to any network.
         * This is set to true under two circumstances:
         * <ul>
         * <li>When connectivity is lost to one network, and there is no other
         * available network to attempt to switch to.</li>
         * <li>When connectivity is lost to one network, and the attempt to
         * switch to another network fails.</li>
         */
        NOT_CONNECTED
    }

    /**
     * Create a new NetworkConnectivityListener.
     */
    public NetworkConnectivityListener() {
        mState = State.UNKNOWN;
        mReceiver = new ConnectivityBroadcastReceiver();
    }

    /**
     * This method starts listening for network connectivity state changes.
     */
    public synchronized NetworkConnectivityListener startListening(Context context) {
        if (!mListening) {
            mContext = context;

            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(mReceiver, filter);
            mListening = true;
        }
        return this;
    }

    /**
     * This method stops this class from listening for network changes.
     */
    public synchronized void stopListening() {
        if (mListening) {
            mContext.unregisterReceiver(mReceiver);
            mContext = null;
            mNetworkInfo = null;
            mOtherNetworkInfo = null;
            mListening = false;
        }
    }

    /**
     * This methods registers a Handler to be called back onto with the
     * specified what code when the network connectivity state changes.
     *
     * @param target The target handler.
     * @param what   The what code to be used when posting a message to the
     *               handler.
     */
    public NetworkConnectivityListener registerHandler(Handler target, int what) {
        mHandlers.put(target, what);
        return this;
    }

    /**
     * This methods unregisters the specified Handler.
     *
     * @param target
     */
    public NetworkConnectivityListener unregisterHandler(Handler target) {
        mHandlers.remove(target);
        return this;
    }

    /**
     * Return the NetworkInfo associated with the most recent connectivity
     * event.
     *
     * @return {@code NetworkInfo} for the network that had the most recent
     *         connectivity event.
     */
    public NetworkInfo getNetworkInfo() {
        return mNetworkInfo;
    }

    /**
     * If the most recent connectivity event was a DISCONNECT, return any
     * information supplied in the broadcast about an alternate network that
     * might be available. If this returns a non-null value, then another
     * broadcast should follow shortly indicating whether connection to the
     * other network succeeded.
     *
     * @return NetworkInfo
     */
    public NetworkInfo getOtherNetworkInfo() {
        return mOtherNetworkInfo;
    }

    public synchronized boolean isConnected() {
        if (mNetworkInfo == null && mContext != null) {
            // obtain current network info if no messages have been received yet
            mNetworkInfo =
                ((ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        }
        return mNetworkInfo != null && mNetworkInfo.isConnected();
    }

    public boolean isWifiConnected() {
        return mNetworkInfo != null
                && mNetworkInfo.isConnected()
                && (mNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI
                || mNetworkInfo.getType() == ConnectivityManager.TYPE_WIMAX);
    }

    private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION) || !mListening) {
                Log.w(TAG, "onReceived() called with " + mState.toString() + " and " + intent);
                return;
            }
            State old = mState;

            boolean noConnectivity = intent.getBooleanExtra(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

            mState =  noConnectivity ? State.NOT_CONNECTED : State.CONNECTED;
            mNetworkInfo = (NetworkInfo) intent
                    .getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            mOtherNetworkInfo = (NetworkInfo) intent
                    .getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

            if (SoundCloudApplication.DEV_MODE) {
                Log.d(TAG, "onReceive(): mNetworkInfo="
                        + mNetworkInfo
                        + " mOtherNetworkInfo = "
                        + (mOtherNetworkInfo == null ? "[none]" : mOtherNetworkInfo + " noConn="
                        + noConnectivity) + " mState=" + mState.toString());
            }

            // Notify any handlers.
            for (Handler target : mHandlers.keySet()) {
                Message message = Message.obtain(target, mHandlers.get(target), old.ordinal(), mState.ordinal());
                target.sendMessage(message);
            }
        }
    }
}
