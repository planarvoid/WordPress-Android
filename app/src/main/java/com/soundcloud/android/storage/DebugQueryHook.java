package com.soundcloud.android.storage;

import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import android.util.Log;

class DebugQueryHook implements PropellerDatabase.QueryHook {

    private static final String TAG = "QueryDebug";
    private static final int MAX_QUERY_LENGTH = 100;

    @Override
    public void onQueryStarted(Query query) {
        logQueryStarted(query.toString());
    }

    @Override
    public void onQueryFinished(Query query, long duration) {
        logQueryFinished(query.toString(), duration);
    }

    @Override
    public void onQueryStarted(String query) {
        logQueryStarted(query);
    }

    @Override
    public void onQueryFinished(String query, long duration) {
        logQueryFinished(query, duration);
    }

    private void logQueryStarted(String query) {
        ErrorUtils.log(Log.DEBUG, TAG, "start : " + limit(query));
    }

    private void logQueryFinished(String query, long duration) {
        ErrorUtils.log(Log.DEBUG, TAG, "finish ("+ duration + "ms) : " + limit(query));
    }

    private String limit(String query) {
        return query.length() <= MAX_QUERY_LENGTH ? query : query.substring(0, MAX_QUERY_LENGTH);
    }
}
