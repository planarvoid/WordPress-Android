package com.soundcloud.android.discovery;

import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.utils.DateProvider;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

class DiscoverySyncer {

    private static final String KEY_LAST_SYNC_TIME = "last_recommendations_sync_time";
    private static final long CACHE_EXPIRATION_TIME = TimeUnit.DAYS.toMillis(1);

    private final SyncInitiator syncInitiator;
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
    DiscoverySyncer(SyncInitiator syncInitiator,
                    @Named(StorageModule.RECOMMENDATIONS_SYNC) SharedPreferences sharedPreferences,
                    DateProvider dateProvider) {
        this.syncInitiator = syncInitiator;
        this.sharedPreferences = sharedPreferences;
        this.dateProvider = dateProvider;
    }

    Observable<Boolean> syncRecommendations() {
        if (isRecommendationsCacheExpired()) {
            return syncInitiator.syncRecommendations().doOnNext(setLastSyncTime).map(FROM_SYNC_RESULT);
        } else {
            return Observable.just(false);
        }
    }

    private boolean isRecommendationsCacheExpired() {
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
