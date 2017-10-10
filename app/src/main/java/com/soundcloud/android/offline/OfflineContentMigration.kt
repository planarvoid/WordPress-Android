package com.soundcloud.android.offline

import android.database.sqlite.SQLiteDatabase
import com.soundcloud.android.commands.PlaylistUrnMapper
import com.soundcloud.android.model.Urn
import com.soundcloud.android.startup.migrations.Migration
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject



class OfflineContentMigration
@Inject
constructor(private val offlineServiceInitiator: OfflineServiceInitiator,
            private val offlineContentStorage: OfflineContentStorage,
            private val trackDownloadStorage: TrackDownloadsStorage,
            private val sqLiteDatabase: SQLiteDatabase) : Migration {

    private val isLegacyOfflineLikesEnabled: Boolean
        get() = !PropellerDatabase(sqLiteDatabase).query(Query.from("OfflineContent").whereEq("_id", 0).whereEq("_type", "2")).isEmpty

    private val legacyOfflinePlaylists: List<Urn>
        get() = PropellerDatabase(sqLiteDatabase).query(Query.from("OfflineContent").select("_id").whereEq("_type", "1")).toList(PlaylistUrnMapper())

    override fun applyMigration() {
        if (isLegacyOfflineLikesEnabled) {
            offlineContentStorage.addLikedTrackCollection().blockingGet()
        }
        offlineContentStorage.storeAsOfflinePlaylists(legacyOfflinePlaylists).blockingGet()
        trackDownloadStorage.writeBulkLegacyInsert(PropellerDatabase(sqLiteDatabase).query(Query.from("TrackDownloads")))
        offlineServiceInitiator.start()
    }

    override fun getApplicableAppVersionCode(): Int = 777
}