package com.soundcloud.android.stations;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.sync.LegacySyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.SyncStateManager;
import rx.Observable;

import android.content.Context;

import javax.inject.Inject;

public class StationsSyncInitiator extends LegacySyncInitiator {
    public static final String RECENT = "RECENT";
    public static final String RECOMMENDATIONS = "RECOMMENDATIONS";

    @Inject
    StationsSyncInitiator(Context context, AccountOperations accountOperations, SyncStateManager syncStateManager) {
        super(context, accountOperations, syncStateManager);
    }

    Observable<SyncResult> syncRecentStations() {
        return requestSyncObservable(RECENT, StationsSyncRequestFactory.Actions.SYNC_RECENT_STATIONS);
    }

    Observable<SyncResult> syncRecommendedStations() {
        return requestSyncObservable(RECOMMENDATIONS, StationsSyncRequestFactory.Actions.SYNC_RECOMMENDED_STATIONS);
    }
}
