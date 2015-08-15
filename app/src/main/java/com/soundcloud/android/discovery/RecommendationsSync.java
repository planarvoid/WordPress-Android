package com.soundcloud.android.discovery;

import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import rx.Observable;
import rx.functions.Func1;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

class RecommendationsSync {

    private static final String KEY_LAST_SYNC_TIME = "last_recommendations_sync_time";
    private static final long CACHE_EXPIRATION_TIME = TimeUnit.DAYS.toMillis(1);

    private final SyncInitiator syncInitiator;
    private final SharedPreferences sharedPreferences;

    private final Func1<SyncResult, Boolean> TO_SYNC_RESULT = new Func1<SyncResult, Boolean>() {
        @Override
        public Boolean call(SyncResult syncResult) {
            if (syncResult.wasSuccess()) {
                updateLastSyncTime();
                return true;
            }
            return false;
        }
    };

    @Inject
    RecommendationsSync(SyncInitiator syncInitiator, @Named(StorageModule.RECOMMENDATIONS_SYNC) SharedPreferences sharedPreferences) {
        this.syncInitiator = syncInitiator;
        this.sharedPreferences = sharedPreferences;
    }

    Observable<Boolean> syncRecommendations() {
        if (isRecommendationsCacheExpired()) {
            return syncInitiator.syncRecommendations().map(TO_SYNC_RESULT);
        } else {
            return Observable.just(false);
        }
    }

    private boolean isRecommendationsCacheExpired() {
        return (System.currentTimeMillis() - getLastSyncTime() > CACHE_EXPIRATION_TIME);
    }

    private long getLastSyncTime() {
        return sharedPreferences.getLong(KEY_LAST_SYNC_TIME, 0);
    }

    private void updateLastSyncTime() {
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis());
        editor.apply();
    }
}