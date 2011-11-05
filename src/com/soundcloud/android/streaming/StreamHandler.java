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
        Log.d(StreamLoader.LOG_TAG, "StreamHandler: handle " + msg.obj);

        StreamItemTask task = (StreamItemTask) msg.obj;
        try {
            Message result = obtainMessage(msg.what, msg.obj);
            if (mWifiLock != null) mWifiLock.acquire();
            final long start = System.currentTimeMillis();
            result.setData(task.execute());
            Log.d(StreamLoader.LOG_TAG, "took "+(System.currentTimeMillis()-start)+ " ms");

            mHandler.sendMessage(result);
        } catch (IOException e) {
            Log.w(StreamLoader.LOG_TAG, e);
            if (task.item.isAvailable() && msg.arg1 < mMaxRetries) {
                Log.d(StreamLoader.LOG_TAG, "retrying, tries=" + msg.arg1);
                final long backoff = msg.arg1*msg.arg1*150;
                sendMessageDelayed(obtainMessage(msg.what, msg.arg1+1, 0, msg.obj), backoff);
            } else {
                Log.d(StreamLoader.LOG_TAG, "giving up (max tries="+mMaxRetries+")");
            }
        } finally {
            if (mWifiLock != null) mWifiLock.release();
        }
    }
}
