package com.soundcloud.android.streaming;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.net.UnknownHostException;

// request pipeline
class StreamHandler extends Handler {
    final private Handler mHandler;
    final private int mMaxRetries;
    private  WifiManager.WifiLock mWifiLock;

    public StreamHandler(Context context, Looper looper, Handler handler, int maxRetries) {
        super(looper);
        mHandler = handler;
        mMaxRetries = maxRetries;

        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        // some phones have really low transfer rates when the screen is turned off, so request a full
        // performance lock on newer devices

        // see http://code.google.com/p/android/issues/detail?id=9781
        mWifiLock = mWifiManager.createWifiLock(
                Build.VERSION.SDK_INT >= 9 ? 3 /* WIFI_MODE_FULL_HIGH_PERF */ : WifiManager.WIFI_MODE_FULL,
                getClass().getSimpleName());
    }

    @Override
    public void handleMessage(Message msg) {
        if (Log.isLoggable(StreamLoader.LOG_TAG, Log.DEBUG))
            Log.d(StreamLoader.LOG_TAG, "StreamHandler: handle " + msg.obj);

        StreamItemTask task = (StreamItemTask) msg.obj;
        try {
            Message result = obtainMessage(msg.what, msg.obj);
            if (mWifiLock != null) mWifiLock.acquire();
            final long start = System.currentTimeMillis();
            result.setData(task.execute());

            if (Log.isLoggable(StreamLoader.LOG_TAG, Log.DEBUG))
                Log.d(StreamLoader.LOG_TAG, "took "+(System.currentTimeMillis()-start)+ " ms");

            mHandler.sendMessage(result);
        } catch (UnknownHostException e) {
            // most likely no connection
            Log.w(StreamLoader.LOG_TAG, "unknown host exception - not retrying");
        } catch (IOException e) {
            Log.w(StreamLoader.LOG_TAG, e);

            final int numTry = msg.arg1;
            if (task.item.isAvailable() && numTry < mMaxRetries) {
                if (Log.isLoggable(StreamLoader.LOG_TAG, Log.DEBUG))
                    Log.d(StreamLoader.LOG_TAG, "retrying, tries=" + numTry);

                final long backoff = numTry*numTry*1000;
                sendMessageDelayed(obtainMessage(msg.what, numTry+1, 0, msg.obj), backoff);
            } else {
                Log.w(StreamLoader.LOG_TAG, "giving up (max tries="+mMaxRetries+")");
            }
        } finally {
            if (mWifiLock != null) mWifiLock.release();
        }
    }
}
