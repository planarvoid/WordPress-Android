package com.soundcloud.android.tasks;

import android.os.AsyncTask;

public abstract class ParallelAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    private static final boolean IS_UNIT_TEST_HACK = !"Dalvik".equals(System.getProperty("java.vm.name"));

    public final AsyncTask<Params, Progress, Result> executeOnThreadPool(Params... params) {
        return IS_UNIT_TEST_HACK ? execute(params) : executeOnExecutor(THREAD_POOL_EXECUTOR, params);
    }
}
