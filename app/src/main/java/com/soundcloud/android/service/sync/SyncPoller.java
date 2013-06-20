package com.soundcloud.android.service.sync;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;

import android.os.Handler;

import javax.annotation.Nullable;
import java.util.List;

public class SyncPoller implements Runnable {
    private static final int POLL_DELAY = 60 * 1000;

    private final Handler mHandler;
    private final Thread mThread;
    private final String mUris;
    private int mPollCount;

    public SyncPoller(Handler handler, Thread thread, List<CollectionSyncRequest> tasks) {
        mHandler = handler;
        mThread = thread;
        mUris = getUriPathsFromTasks(tasks);
    }

    public void schedule(){
        mHandler.postDelayed(this, POLL_DELAY);
    }

    public void stop() {
        mHandler.removeCallbacks(this);
    }

    @Override
    public void run() {
        mPollCount++;
        final String msg = "[" + mPollCount + "] " + mUris;
        SoundCloudApplication.handleSilentException(msg, new SyncPollData(msg, mThread.getStackTrace()));
        schedule();
    }

    private String getUriPathsFromTasks(List<CollectionSyncRequest> tasks) {
        final List<String> uriList = Lists.transform(tasks, new Function<CollectionSyncRequest, String>() {
            @Nullable
            @Override
            public String apply(@Nullable CollectionSyncRequest input) {
                return input.contentUri.getPath();
            }
        });
        return Joiner.on(",").join(uriList);
    }

    private static class SyncPollData extends Exception {
        private SyncPollData(String message, StackTraceElement[] stackTraceElements) {
            super(message);
            setStackTrace(stackTraceElements);
        }

        @Override
        public String toString() {
            return "SyncPollData :" + getMessage();
        }
    }
}
