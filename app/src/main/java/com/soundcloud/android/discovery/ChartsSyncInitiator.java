package com.soundcloud.android.discovery;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

public class ChartsSyncInitiator extends SyncInitiator {
    public static final String TYPE = "CHARTS";

    private static final String KEY_LAST_SYNC_TIME = "last_charts_sync_time";
    private static final long CACHE_EXPIRATION_TIME = TimeUnit.DAYS.toMillis(1);
    private static final String SYNC_CHARTS_ACTION = "syncCharts";
    private static final Func1<Throwable, Observable<SyncResult>> RESUME_ON_SYNC_FAILURE = new Func1<Throwable, Observable<SyncResult>>() {
        @Override
        public Observable<SyncResult> call(Throwable throwable) {
            return Observable.just(SyncResult.failure(SYNC_CHARTS_ACTION, new Exception(throwable)));
        }
    };

    private final SharedPreferences sharedPreferences;
    private final DateProvider dateProvider;

    private static final Func1<SyncResult, Boolean> FROM_SYNC_RESULT = new Func1<SyncResult, Boolean>() {
        @Override
        public Boolean call(SyncResult syncResult) {
            return syncResult.wasSuccess();
        }
    };

    private final Action1<SyncResult> setLastSyncTime = new Action1<SyncResult>() {
        @Override
        public void call(SyncResult syncResult) {
            if (syncResult.wasSuccess()) {
                updateLastSyncTime();
            }
        }
    };

    @Inject
    ChartsSyncInitiator(Context context,
                        AccountOperations accountOperations,
                        SyncStateManager syncStateManager,
                        @Named(StorageModule.CHARTS_SYNC) SharedPreferences sharedPreferences,
                        CurrentDateProvider dateProvider) {
        super(context, accountOperations, syncStateManager);
        this.sharedPreferences = sharedPreferences;
        this.dateProvider = dateProvider;
    }

    Observable<Boolean> syncCharts() {
        if (isChartsCacheExpired()) {
            return requestSyncObservable(TYPE, ChartsSyncRequestFactory.Actions.SYNC_CHARTS)
                    .doOnNext(setLastSyncTime)
                    .map(FROM_SYNC_RESULT);
        } else {
            return Observable.just(false);
        }
    }

    void clearLastSyncTime() {
        sharedPreferences.edit().clear().apply();
    }

    private boolean isChartsCacheExpired() {
        return (dateProvider.getCurrentTime() - getLastSyncTime() > CACHE_EXPIRATION_TIME);
    }

    private long getLastSyncTime() {
        return sharedPreferences.getLong(KEY_LAST_SYNC_TIME, 0);
    }

    private void updateLastSyncTime() {
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(KEY_LAST_SYNC_TIME, dateProvider.getCurrentTime());
        editor.apply();
    }
}
