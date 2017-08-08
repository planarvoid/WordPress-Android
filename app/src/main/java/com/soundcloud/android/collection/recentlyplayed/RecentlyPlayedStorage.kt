package com.soundcloud.android.collection.recentlyplayed

import com.soundcloud.android.collection.playhistory.PlayHistoryRecord
import com.soundcloud.android.storage.Tables.RecentlyPlayed
import com.soundcloud.propeller.CursorReader
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.TxnResult
import com.soundcloud.propeller.query.ColumnFunctions.count
import com.soundcloud.propeller.query.ColumnFunctions.max
import com.soundcloud.propeller.query.Filter.filter
import com.soundcloud.propeller.query.Query
import com.soundcloud.propeller.query.Where
import com.soundcloud.propeller.rx.PropellerRxV2
import com.soundcloud.propeller.schema.BulkInsertValues
import io.reactivex.Single
import javax.inject.Inject

open class RecentlyPlayedStorage
@Inject
constructor(private val database: PropellerDatabase, private val propellerRx: PropellerRxV2) {

    companion object {
        private const val COLUMN_MAX_TIMESTAMP = "max_timestamp"
        private val PLAY_HISTORY_RECORD_RESULT_MAPPER = { reader: CursorReader ->
            val contextType = reader.getInt(RecentlyPlayed.CONTEXT_TYPE)
            val contextId = reader.getLong(RecentlyPlayed.CONTEXT_ID)
            val timestamp = reader.getLong(COLUMN_MAX_TIMESTAMP)
            val contextUrn = PlayHistoryRecord.contextUrnFor(contextType, contextId)
            PlayHistoryRecord.forRecentlyPlayed(timestamp, contextUrn)
        }
    }

    open fun loadRecentlyPlayed(limit: Int): Single<List<PlayHistoryRecord>> {
        return propellerRx.queryResult(buildRecentlyPlayedQuery(limit))
                .map { result -> result.toList(PLAY_HISTORY_RECORD_RESULT_MAPPER) }
                .firstOrError()
    }

    private fun buildRecentlyPlayedQuery(limit: Int): Query {
        return Query.from(RecentlyPlayed.TABLE)
                .select(RecentlyPlayed.CONTEXT_ID, RecentlyPlayed.CONTEXT_TYPE, max(RecentlyPlayed.TIMESTAMP).`as`(COLUMN_MAX_TIMESTAMP))
                .order(RecentlyPlayed.TIMESTAMP, Query.Order.DESC)
                .groupBy(RecentlyPlayed.CONTEXT_TYPE, RecentlyPlayed.CONTEXT_ID)
                .limit(limit)
    }

    open fun loadUnSyncedRecentlyPlayed() = syncedRecentlyPlayed(false)

    open fun loadSyncedRecentlyPlayed() = syncedRecentlyPlayed(true)

    open fun setSynced(playHistoryRecords: List<PlayHistoryRecord>) = database.bulkInsert(RecentlyPlayed.TABLE, buildBulkValues(playHistoryRecords))

    open fun insertRecentlyPlayed(addRecords: List<PlayHistoryRecord>) = database.bulkInsert(RecentlyPlayed.TABLE, buildBulkValues(addRecords))

    open fun removeRecentlyPlayed(removeRecords: List<PlayHistoryRecord>): TxnResult {
        return database.runTransaction(object : PropellerDatabase.Transaction() {
            override fun steps(propeller: PropellerDatabase) {
                for (removeRecord in removeRecords) {
                    step(database.delete(RecentlyPlayed.TABLE, buildMatchFilter(removeRecord)))
                    if (!success()) {
                        break
                    }
                }
            }
        })
    }

    open fun hasPendingContextsToSync(): Boolean {
        val query = Query.from(RecentlyPlayed.TABLE)
                .select(count(RecentlyPlayed.CONTEXT_ID))
                .whereEq(RecentlyPlayed.SYNCED, false)

        return database.query(query).first { it.getInt(0) } > 0
    }

    open fun clear() {
        database.delete(RecentlyPlayed.TABLE)
    }

    private fun syncedRecentlyPlayed(synced: Boolean): List<PlayHistoryRecord> {
        return database.query(loadSyncedRecentlyPlayedQuery(synced))
                .toList { reader ->
                    val contextType = reader.getInt(RecentlyPlayed.CONTEXT_TYPE)
                    val contextId = reader.getLong(RecentlyPlayed.CONTEXT_ID)
                    val contextUrn = PlayHistoryRecord.contextUrnFor(contextType, contextId)
                    val timestamp = reader.getLong(RecentlyPlayed.TIMESTAMP)
                    PlayHistoryRecord.forRecentlyPlayed(timestamp, contextUrn)
                }
    }

    private fun loadSyncedRecentlyPlayedQuery(synced: Boolean): Query {
        return Query.from(RecentlyPlayed.TABLE)
                .select(RecentlyPlayed.TIMESTAMP, RecentlyPlayed.CONTEXT_TYPE, RecentlyPlayed.CONTEXT_ID)
                .whereEq(RecentlyPlayed.SYNCED, synced)
    }

    private fun buildBulkValues(records: Collection<PlayHistoryRecord>): BulkInsertValues {
        val builder = BulkInsertValues.Builder(
                listOf(
                        RecentlyPlayed.TIMESTAMP,
                        RecentlyPlayed.CONTEXT_ID,
                        RecentlyPlayed.CONTEXT_TYPE,
                        RecentlyPlayed.SYNCED
                )
        )

        for (record in records) {
            builder.addRow(listOf(record.timestamp(), record.contextUrn().numericId, record.contextType, true))
        }
        return builder.build()
    }

    private fun buildMatchFilter(record: PlayHistoryRecord): Where {
        return filter()
                .whereEq(RecentlyPlayed.TIMESTAMP, record.timestamp())
                .whereEq(RecentlyPlayed.CONTEXT_TYPE, record.contextType)
                .whereEq(RecentlyPlayed.CONTEXT_ID, record.contextUrn().numericId)
    }

}
