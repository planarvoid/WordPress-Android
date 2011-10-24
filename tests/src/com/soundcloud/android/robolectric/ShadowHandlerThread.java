package com.soundcloud.android.robolectric;

import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

import android.os.HandlerThread;
import android.os.Looper;


/** @noinspection ALL*/
@Implements(HandlerThread.class)
public class ShadowHandlerThread /* extends Thread */ {

    public void __constructor__(String name) {
    }

    public void __constructor__(String name, int priority) {
        __constructor__(name);
    }


    @Implementation
    public Looper getLooper() {
        return Looper.myLooper();
    }

    @Implementation
    public boolean quit() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quit();
            return true;
        }
        return false;
    }

    @Implementation
    public int getThreadId() {
        return -1;
    }
}
