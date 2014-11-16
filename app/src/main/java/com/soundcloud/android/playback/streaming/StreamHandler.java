package com.soundcloud.android.playback.streaming;

import com.soundcloud.android.utils.IOUtils;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;

// request pipeline
class StreamHandler extends Handler {
    private final Handler handler;
    private final int maxRetries;
    private final WifiManager.WifiLock wifiLock;

    public StreamHandler(Context context, Looper looper, Handler handler, int maxRetries) {
        super(looper);
        this.handler = handler;
        this.maxRetries = maxRetries;
        wifiLock = IOUtils.createHiPerfWifiLock(context, getClass().getSimpleName());
    }

    @Override
    public void handleMessage(Message msg) {
        if (Log.isLoggable(StreamLoader.LOG_TAG, Log.DEBUG)) {
            Log.d(StreamLoader.LOG_TAG, "StreamHandler: handle " + msg.obj);
        }

        StreamItemTask task = (StreamItemTask) msg.obj;
        try {
            final Message result = obtainMessage(msg.what, msg.obj);

            if (wifiLock != null) {
                wifiLock.acquire();
            }
            final long start = System.currentTimeMillis();
            result.setData(task.execute());
            if (Log.isLoggable(StreamLoader.LOG_TAG, Log.DEBUG)) {
                Log.d(StreamLoader.LOG_TAG, "took " + (System.currentTimeMillis() - start) + " ms");
            }

            handler.sendMessage(result);
        } catch (IOException e) {
            Log.w(StreamLoader.LOG_TAG, e);
            final int numTry = msg.arg1;
            if (task.item.isAvailable() && numTry < maxRetries) {
                if (Log.isLoggable(StreamLoader.LOG_TAG, Log.DEBUG)) {
                    Log.d(StreamLoader.LOG_TAG, "retrying, tries=" + numTry);
                }

                final long backoff = numTry * numTry * 1000;
                sendMessageDelayed(obtainMessage(msg.what, numTry + 1, 0, msg.obj), backoff);
            } else {
                Log.w(StreamLoader.LOG_TAG, "giving up (max tries=" + maxRetries + ")");

                // assume we are not connected, return item to queue and wait
                // to have the connection again
                handler.sendMessage(obtainMessage(msg.what, msg.obj));
            }
        } finally {
            if (wifiLock != null) {
                wifiLock.release();
            }
        }
    }
}
