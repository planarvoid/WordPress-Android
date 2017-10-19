package com.soundcloud.android.offline

import com.soundcloud.android.model.Urn
import com.soundcloud.android.offline.TrackDownloadsDbModel.FACTORY
import com.soundcloud.android.utils.CurrentDateProvider
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.android.utils.extensions.partition
import com.soundcloud.android.utils.extensions.toDate
import com.soundcloud.java.collections.Lists
import com.soundcloud.propeller.CursorReader
import com.soundcloud.propeller.QueryResult
import com.squareup.sqlbrite2.BriteDatabase
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@OpenForTesting
class TrackDownloadsStorage
@Inject
constructor(private val dateProvider: CurrentDateProvider, private val offlineDatabase: OfflineDatabase) {

    fun writeBulkLegacyInsert(queryResult: QueryResult): BriteDatabase.Transaction {
        return offlineDatabase.runInTransaction {
            val insert = TrackDownloadsModel.InsertRow(offlineDatabase.writableDatabase(), TrackDownloadsDbModel.FACTORY)
            for (cursorReader in queryResult.iterator()) {
                insert.bind(
                        Urn.forTrack(cursorReader.getLong(0)),
                        nullOrLong(cursorReader, 1),
                        nullOrLong(cursorReader, 2),
                        nullOrLong(cursorReader, 3),
                        nullOrLong(cursorReader, 4)
                )
                offlineDatabase.insert(TrackDownloadsModel.TABLE_NAME, insert.program)
            }
        }
    }

    fun nullOrLong(cursorReader: CursorReader, columnIndex: Int): Long? {
        return if (cursorReader.isNotNull(columnIndex)) {
            cursorReader.getLong(columnIndex)
        } else {
            null
        }
    }

    fun writeUpdates(offlineContentUpdates: OfflineContentUpdates): Single<BriteDatabase.Transaction> {
        return offlineDatabase.runInTransactionAsync {

            val insert = TrackDownloadsModel.InsertRowWithRequestedAt(offlineDatabase.writableDatabase(), TrackDownloadsDbModel.FACTORY)
            for (urn in offlineContentUpdates.newTracksToDownload()) {
                insert.bind(urn, dateProvider.currentTime)
                offlineDatabase.insert(TrackDownloadsModel.TABLE_NAME, insert.program)
            }

            val removal = TrackDownloadsModel.MarkForRemoval(offlineDatabase.writableDatabase(), TrackDownloadsDbModel.FACTORY)
            for (urn in offlineContentUpdates.tracksToRemove()) {
                removal.bind(dateProvider.currentTime, urn)
                failFastOnMissingItem(offlineDatabase.updateOrDelete(TrackDownloadsModel.TABLE_NAME, removal.program), urn)
            }

            val restore = TrackDownloadsModel.MarkDownloaded(offlineDatabase.writableDatabase(), TrackDownloadsDbModel.FACTORY)
            for (urn in offlineContentUpdates.tracksToRestore()) {
                restore.bind(dateProvider.currentTime, urn)
                failFastOnMissingItem(offlineDatabase.updateOrDelete(TrackDownloadsModel.TABLE_NAME, restore.program), urn)
            }

            // NOTE : This is an insert or replace, as we cannot guarantee it does not already exist in the table, unlike remove / restore
            val unavailable = TrackDownloadsModel.MarkUnavailable(offlineDatabase.writableDatabase(), TrackDownloadsDbModel.FACTORY)
            for (urn in offlineContentUpdates.unavailableTracks()) {
                unavailable.bind(urn, dateProvider.currentTime)
                offlineDatabase.insert(TrackDownloadsModel.TABLE_NAME, unavailable.program)
            }
        }
    }

    fun deleteWithUrn(urn: Urn): Single<Long> {
        val deleteRow = TrackDownloadsModel.DeleteRow(offlineDatabase.writableDatabase(), TrackDownloadsDbModel.FACTORY)
        deleteRow.bind(urn)
        return offlineDatabase.updateOrDeleteAsync(TrackDownloadsModel.TABLE_NAME, deleteRow.program)
    }

    fun deleteAllDownloads(): Single<Long> {
        val deleteAll = TrackDownloadsModel.DeleteAll(offlineDatabase.writableDatabase())
        return offlineDatabase.updateOrDeleteAsync(TrackDownloadsModel.TABLE_NAME, deleteAll.program)
    }

    private fun failFastOnMissingItem(updateOrDelete: Long, urn: Urn?) {
        if (updateOrDelete < 1) {
            throw IllegalArgumentException("Unable to commit updates, item not present in downloads table: $urn")
        }
    }

    fun offlineStates(): Single<Map<Urn, OfflineState>> {
        return offlineDatabase.executeAsyncQuery(FACTORY.selectAll(), FACTORY.selectAllMapper())
                .map(this::dbModelsToOfflineStates)
    }

    fun onlyOfflineTracks(tracks: List<Urn>): List<Urn> {
        return tracks
                .partition(DEFAULT_BATCH_SIZE)
                .map { queryOfflineTracksBatch(it) }
                .fold(ArrayList(), { acc, values -> acc.addAll(values); acc })
    }

    private fun queryOfflineTracksBatch(it: List<Urn>): List<Urn> {
        return offlineDatabase.executeQuery(FACTORY.selectDownloaded(it.toTypedArray()),
                FACTORY.selectDownloadedMapper()).toUrns()
    }

    fun getOfflineStates(tracks: Collection<Urn>): Single<Map<Urn, OfflineState>> {
        return Observable.fromIterable(ArrayList<Urn>(tracks).partition(DEFAULT_BATCH_SIZE))
                .flatMapSingle(this::getOfflineStateBatch)
                .collect({ HashMap() }, { map: HashMap<Urn, OfflineState>, modelList: List<TrackDownloadsDbModel> -> dbModelsToOfflineStates(modelList, map) })
                .map { it }

    }

    private fun getOfflineStateBatch(it: List<Urn>) = offlineDatabase.executeAsyncQuery(FACTORY.selectBatch(it.toTypedArray()), FACTORY.selectBatchMapper())

    val tracksToRemove: Single<List<Urn>>
        get() {
            val removalDelayedTimestamp = dateProvider.currentTime - DELAY_BEFORE_REMOVAL
            return offlineDatabase.executeAsyncQuery(FACTORY.selectWithRemovalDateBefore(removalDelayedTimestamp),
                    FACTORY.selectWithRemovalDateBeforeMapper())
                    .map { it.toUrns() }

        }

    val unavailableTracks: Single<List<Urn>>
        get() {
            return offlineDatabase.executeAsyncQuery(FACTORY.selectUnavailable(),
                    FACTORY.selectWithRemovalDateBeforeMapper())
                    .map { it.toUrns() }

        }

    val requestedTracks: Single<List<Urn>>
        get() {
            return offlineDatabase.executeAsyncQuery(FACTORY.selectRequested(),
                    FACTORY.selectWithRemovalDateBeforeMapper())
                    .map { it.toUrns() }

        }

    val downloadedTracks: Single<List<Urn>>
        get() {
            return offlineDatabase.executeAsyncQuery(FACTORY.selectAllDownloaded(),
                    FACTORY.selectWithRemovalDateBeforeMapper())
                    .map { it.toUrns() }

        }

    val resetTracksToRequested: Completable
        get() {
            return offlineDatabase.updateOrDeleteAsync(TrackDownloadsModel.TABLE_NAME,
                    TrackDownloadsModel.UpdateAllForRedownload(offlineDatabase.writableDatabase()).program)
                    .toCompletable()
        }

    fun storeCompletedDownload(downloadState: DownloadState): Boolean {
        val markCompletedStatement = TrackDownloadsModel.MarkDownloaded(offlineDatabase.writableDatabase(), TrackDownloadsDbModel.FACTORY)
        markCompletedStatement.bind(downloadState.timestamp, downloadState.track)
        return markCompletedStatement.program.executeUpdateDelete() > 0
    }

    fun markTrackAsUnavailable(track: Urn): Boolean {
        val markUnavailable = TrackDownloadsModel.MarkUnavailable(offlineDatabase.writableDatabase(), TrackDownloadsDbModel.FACTORY)
        markUnavailable.bind(track, dateProvider.currentTime)
        return markUnavailable.program.executeUpdateDelete() > 0
    }

    private fun dbModelsToOfflineStates(it: List<TrackDownloadsDbModel>): HashMap<Urn, OfflineState> = dbModelsToOfflineStates(it, HashMap())

    private fun dbModelsToOfflineStates(modelList: List<TrackDownloadsDbModel>,
                                        map: HashMap<Urn, OfflineState>): HashMap<Urn, OfflineState> {
        for (trackDownloadsDbModel in modelList) {
            map.put(trackDownloadsDbModel.urn(), getOfflineState(
                    true,
                    trackDownloadsDbModel.requested_at()?.toDate() ?: Date(0),
                    trackDownloadsDbModel.removed_at()?.toDate() ?: Date(0),
                    trackDownloadsDbModel.downloaded_at()?.toDate() ?: Date(0),
                    trackDownloadsDbModel.unavailable_at()?.toDate() ?: Date(0)

            ))
        }
        return map
    }

    private fun List<TrackDownloadsDbModel>.toUrns(): List<Urn> = Lists.transform(this, { it.urn() })

    companion object {
        private val DELAY_BEFORE_REMOVAL = TimeUnit.MINUTES.toMillis(3)
        private val DEFAULT_BATCH_SIZE = 500 // default SQL var limit is 999. Being safe

        private fun getOfflineState(unavailableEnabled: Boolean,
                                    requestedAt: Date,
                                    removedAt: Date,
                                    downloadedAt: Date,
                                    unavailableAt: Date): OfflineState {
            return if (isMostRecentDate(requestedAt, removedAt, downloadedAt, unavailableAt)) {
                OfflineState.REQUESTED
            } else if (isMostRecentDate(downloadedAt, requestedAt, removedAt, unavailableAt)) {
                OfflineState.DOWNLOADED
            } else if (unavailableEnabled && isMostRecentDate(unavailableAt, requestedAt, removedAt, downloadedAt)) {
                OfflineState.UNAVAILABLE
            } else {
                OfflineState.NOT_OFFLINE
            }
        }

        private fun isMostRecentDate(dateToTest: Date, vararg dates: Date): Boolean = dates.none { it.after(dateToTest) || it == dateToTest }
    }
}
