package com.soundcloud.android.discovery;

import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.sync.LegacySyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

class RecommendedTracksSyncInitiator {

    private static final String KEY_LAST_SYNC_TIME = "last_recommendations_sync_time";
    private static final long CACHE_EXPIRATION_TIME = TimeUnit.DAYS.toMillis(1);
    private static final String SYNC_RECOMMENDATIONS_ACTION = "sync";
    private static final Func1<Throwable, Observable<SyncJobResult>> RESUME_ON_SYNC_FAILURE = new Func1<Throwable, Observable<SyncJobResult>>() {
        @Override
        public Observable<SyncJobResult> call(Throwable throwable) {
            return Observable.just(SyncJobResult.failure(SYNC_RECOMMENDATIONS_ACTION, new Exception(throwable)));
        }
    };

    private final LegacySyncInitiator syncInitiator;
    private final SharedPreferences sharedPreferences;
    private final DateProvider dateProvider;

    private static final Func1<SyncJobResult, Boolean> FROM_SYNC_RESULT = new Func1<SyncJobResult, Boolean>() {
        @Override
        public Boolean call(SyncJobResult syncJobResult) {
            return syncJobResult.wasSuccess();
        }
    };

    private final Action1<SyncJobResult> setLastSyncTime = new Action1<SyncJobResult>() {
        @Override
        public void call(SyncJobResult syncJobResult) {
            if (syncJobResult.wasSuccess()) {
                updateLastSyncTime();
            }
        }
    };

    @Inject
    RecommendedTracksSyncInitiator(LegacySyncInitiator syncInitiator,
                                   @Named(StorageModule.RECOMMENDED_TRACKS_SYNC) SharedPreferences sharedPreferences,
                                   CurrentDateProvider dateProvider) {
        this.syncInitiator = syncInitiator;
        this.sharedPreferences = sharedPreferences;
        this.dateProvider = dateProvider;
    }

    Observable<Boolean> sync() {
        if (isCacheExpired()) {
            return syncInitiator.syncRecommendedTracks().onErrorResumeNext(RESUME_ON_SYNC_FAILURE).doOnNext(setLastSyncTime).map(FROM_SYNC_RESULT);
        } else {
            return Observable.just(false);
        }
    }

    void clearLastSyncTime() {
        sharedPreferences.edit().clear().apply();
    }

    private boolean isCacheExpired() {
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
