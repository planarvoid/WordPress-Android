package com.soundcloud.android.collection.recentlyplayed

import android.database.Cursor
import com.soundcloud.android.collection.CollectionDatabase
import com.soundcloud.android.collection.DbModel.RecentlyPlayed
import com.soundcloud.android.collection.DbModel.RecentlyPlayed.FACTORY
import com.soundcloud.android.collection.playhistory.PlayHistoryRecord
import com.soundcloud.android.utils.OpenForTesting
import com.squareup.sqldelight.RowMapper
import io.reactivex.Single
import javax.inject.Inject

@Suppress("TooManyFunctions")
@OpenForTesting
class RecentlyPlayedStorage
@Inject
constructor(private val collectionDatabase: CollectionDatabase) {

    private val playHistoryRecordMapper = { cursor: Cursor ->
        val contextType = cursor.getAsInt(RecentlyPlayed.CONTEXT_TYPE)
        val contextId = cursor.getAsLong(RecentlyPlayed.CONTEXT_ID)
        val timestamp = cursor.getAsLong(RecentlyPlayed.TIMESTAMP)
        val contextUrn = PlayHistoryRecord.contextUrnFor(contextType, contextId)
        PlayHistoryRecord.forRecentlyPlayed(timestamp, contextUrn)
    }

    internal fun loadAll() = collectionDatabase.executeQuery(FACTORY.selectAll(), FACTORY.selectAllMapper())

    fun loadRecentlyPlayed(limit: Int): Single<List<PlayHistoryRecord>> {
        return collectionDatabase.executeAsyncQuery(FACTORY.selectRecentlyPlayed(), RowMapper(playHistoryRecordMapper))
    }

    fun loadContextIdsByType(contextType: Int): Set<Long> {
        return collectionDatabase.executeQuery(FACTORY.selectIdsByContextType(contextType.toLong()), { cursor -> cursor.getLong(0) }).toSet()
    }

    fun loadUnSyncedRecentlyPlayed() = loadBySyncStatus(false)

    fun loadSyncedRecentlyPlayed() = loadBySyncStatus(true)

    fun markAsSynced(records: List<PlayHistoryRecord>) = bulkInsert(records)

    fun insertRecentlyPlayed(records: List<PlayHistoryRecord>) = bulkInsert(records)

    fun upsertRow(record: PlayHistoryRecord) {
        val statement = RecentlyPlayedModel.UpsertRow(collectionDatabase.writableDatabase())
        statement.bind(record.contextUrn().numericId, record.contextType.toLong(), record.timestamp())
        collectionDatabase.insert(RecentlyPlayed.TABLE_NAME, statement.program)
    }

    fun removeRecentlyPlayed(records: List<PlayHistoryRecord>) {
        collectionDatabase.runInTransaction {
            val statement = RecentlyPlayedModel.DeleteRecentlyPlayed(collectionDatabase.writableDatabase())
            for (record in records) {
                statement.bind(record.contextUrn().numericId, record.contextType.toLong(), record.timestamp())
                collectionDatabase.updateOrDelete(RecentlyPlayed.TABLE_NAME, statement.program)
            }
        }
    }

    fun hasPendingContextsToSync(): Boolean {
        val resultList = collectionDatabase.executeQuery(FACTORY.unsyncedRecentlyPlayedCount(), FACTORY.unsyncedRecentlyPlayedCountMapper())
        val unsyncedRecentlyPlayedCount = resultList[0]
        return unsyncedRecentlyPlayedCount > 0
    }

    fun clear() {
        collectionDatabase.clear(RecentlyPlayed.TABLE_NAME)
    }

    fun trim(limit : Int) {
        val statement = RecentlyPlayedModel.Trim(collectionDatabase.writableDatabase())
        statement.bind(limit.toLong())
        collectionDatabase.updateOrDelete(RecentlyPlayedModel.TABLE_NAME, statement.program)
    }

    private fun bulkInsert(records: List<PlayHistoryRecord>) {
        collectionDatabase.runInTransaction {
            val statement = RecentlyPlayedModel.InsertRow(collectionDatabase.writableDatabase())
            for (record in records) {
                statement.bind(record.contextUrn().numericId, record.contextType.toLong(), record.timestamp(), true)
                collectionDatabase.insert(RecentlyPlayed.TABLE_NAME, statement.program)
            }
        }
    }

    private fun loadBySyncStatus(synced: Boolean): List<PlayHistoryRecord> {
        return collectionDatabase.executeQuery(FACTORY.selectRecentlyPlayedBySyncStatus(synced), RowMapper(playHistoryRecordMapper))
    }

    fun Cursor.getAsInt(columnName: String) = getInt(getColumnIndex(columnName))
    fun Cursor.getAsLong(columnName: String) = getLong(getColumnIndex(columnName))

}
