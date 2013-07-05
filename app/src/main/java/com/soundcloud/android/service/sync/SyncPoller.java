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
    private final Thread mWatchedThread;
    private final String mSyncTargetUris;
    private int mPollCount;

    public SyncPoller(Handler handler, Thread watchedThread, List<CollectionSyncRequest> syncTasks) {
        mHandler = handler;
        mWatchedThread = watchedThread;
        mSyncTargetUris = getUriPathsFromTasks(syncTasks);
    }

    public void schedule(){
        mHandler.postDelayed(this, POLL_DELAY);
    }

    public void stop() {
        mHandler.removeCallbacks(this);
    }

    @Override
    public void run() {
        final String msg = "[" + (++mPollCount) + "] " + mSyncTargetUris;
        SoundCloudApplication.handleSilentException(msg, new SyncPollData(msg, mWatchedThread.getStackTrace()));
        schedule();
    }

    private String getUriPathsFromTasks(List<CollectionSyncRequest> tasks) {
        final List<String> uriList = Lists.transform(tasks, new Function<CollectionSyncRequest, String>() {
            @Nullable
            @Override
            public String apply(@Nullable CollectionSyncRequest input) {
                return input.getContentUri().getPath();
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
