package com.soundcloud.android.utils;


import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import java.util.ArrayList;

/**
 * Proxy {@link ResultReceiver} that offers a listener interface that can be
 * detached. Useful for when sending callbacks to a {@link Service} where a
 * listening {@link Activity} can be swapped out during configuration changes.
 */
public class DetachableResultReceiver extends ResultReceiver {
    private static final String TAG = "DetachableResultReceiver";

    private Receiver mReceiver;
    private ArrayList<PendingResult> mPendingResults;
    private class PendingResult {
        int resultCode;
        Bundle resultData;
        public PendingResult(int resultCode, Bundle resultData){
            this.resultCode = resultCode;
            this.resultData = resultData;
        }
    }

    public DetachableResultReceiver(Handler handler) {
        super(handler);
        mPendingResults = new ArrayList<PendingResult>();
    }

    public void clearReceiver() {
        mReceiver = null;
    }

    public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
        if (mPendingResults.size() > 0){
            for (PendingResult pr : mPendingResults){
                mReceiver.onReceiveResult(pr.resultCode, pr.resultData);
            }
            mPendingResults.clear();
        }
    }

    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle resultData);
    }

    private void addPendingResult(int resultCode, Bundle resultData) {
        mPendingResults.add(new PendingResult(resultCode,resultData));
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        } else {
            addPendingResult(resultCode,resultData);
        }
    }
}
