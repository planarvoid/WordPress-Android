package com.soundcloud.android.startup.migrations

import com.soundcloud.android.sync.SyncInitiatorBridge
import javax.inject.Inject

class FollowingMigration
@Inject
constructor(val syncInitiatorBridge: SyncInitiatorBridge) : Migration {
    override fun applyMigration() {
        syncInitiatorBridge.refreshFollowings()
    }

    override fun getApplicableAppVersionCode(): Int = 777

}
