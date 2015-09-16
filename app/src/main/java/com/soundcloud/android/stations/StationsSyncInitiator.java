package com.soundcloud.android.stations;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.SyncStateManager;
import rx.Observable;

import android.content.Context;

import javax.inject.Inject;

public class StationsSyncInitiator extends SyncInitiator {
    public static final String TYPE = "STATIONS";

    @Inject
    StationsSyncInitiator(Context context, AccountOperations accountOperations, SyncStateManager syncStateManager) {
        super(context, accountOperations, syncStateManager);
    }

    Observable<SyncResult> syncRecentStations() {
        return requestSyncObservable(TYPE, StationsSyncRequestFactory.Actions.ACTION_SYNC_STATIONS);
    }
}
