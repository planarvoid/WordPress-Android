package com.soundcloud.android.utils;


import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import java.util.ArrayList;
import java.util.List;

/**
 * Proxy {@link ResultReceiver} that offers a listener interface that can be
 * detached. Useful for when sending callbacks to a {@link android.app.Service} where a
 * listening {@link android.app.Activity} can be swapped out during configuration changes.
 */
public class DetachableResultReceiver extends ResultReceiver {

    private Receiver receiver;
    private final List<PendingResult> pendingResults;
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
        pendingResults = new ArrayList<>();
    }

    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
        if (pendingResults.size() > 0){
            for (PendingResult pr : pendingResults){
                this.receiver.onReceiveResult(pr.resultCode, pr.resultData);
            }
            pendingResults.clear();
        }
    }

    public interface Receiver {
        void onReceiveResult(int resultCode, Bundle resultData);
    }

    private void addPendingResult(int resultCode, Bundle resultData) {
        pendingResults.add(new PendingResult(resultCode, resultData));
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (receiver != null) {
            receiver.onReceiveResult(resultCode, resultData);
        } else {
            addPendingResult(resultCode,resultData);
        }
    }
}
