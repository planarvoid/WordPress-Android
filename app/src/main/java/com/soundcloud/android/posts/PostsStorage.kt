package com.soundcloud.android.posts

import com.soundcloud.android.model.Association
import com.soundcloud.android.model.Urn
import com.soundcloud.android.playlists.RemovePlaylistCommand
import com.soundcloud.android.storage.Table
import com.soundcloud.android.storage.TableColumns
import com.soundcloud.android.storage.Tables
import com.soundcloud.android.utils.CurrentDateProvider
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.propeller.ChangeResult
import com.soundcloud.propeller.ContentValuesBuilder
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.TxnResult
import com.soundcloud.propeller.query.Filter
import com.soundcloud.propeller.query.Query
import com.soundcloud.propeller.rx.PropellerRxV2
import io.reactivex.Single
import javax.inject.Inject

@OpenForTesting
class PostsStorage
@Inject
constructor(private val propellerRx: PropellerRxV2,
            private val dateProvider: CurrentDateProvider,
            private val removePlaylistCommand: RemovePlaylistCommand) {

    fun loadPostedTracksSortedByDateDesc(): Single<List<Association>> {
        return propellerRx
                .queryResult(loadPostedTracksSortedByDateDescQuery())
                .map { it.toList { Association(Urn.forTrack(it.getLong(Tables.Posts.TARGET_ID)), it.getDateFromTimestamp(Tables.Posts.CREATED_AT)) } }
                .singleOrError()
    }

    fun loadPostedPlaylists(limit: Int, fromTimestamp: Long): Single<List<Association>> {
        return propellerRx
                .queryResult(playlistPostQuery(limit, fromTimestamp))
                .map {
                    it.toList { cursorReader ->
                        Association(Urn.forPlaylist(cursorReader.getLong(Tables.Posts.TARGET_ID)),
                                    cursorReader.getDateFromTimestamp(Tables.Posts.CREATED_AT))
                    }
                }
                .singleOrError()
    }

    fun loadPostedPlaylists(limit: Int): Single<List<Association>> {
        return loadPostedPlaylists(limit, java.lang.Long.MAX_VALUE)
    }

    fun markPlaylistPendingRemoval(urn: Urn): Single<TxnResult> {
        require(urn.isPlaylist) { "urn argument: $urn is not a playlist urn" }

        return propellerRx.runTransaction(object : PropellerDatabase.Transaction() {
            override fun steps(propeller: PropellerDatabase) {
                removePlaylistFromSoundsTable(propeller)
                removePlaylistFromPostsTable(propeller)
                removePlaylistFromActivitiesView(propeller)
                removePlaylistFromSoundStreamView(propeller)
            }

            private fun removePlaylistFromSoundsTable(propeller: PropellerDatabase) {
                step<ChangeResult>(propeller.update(
                        Tables.Sounds.TABLE,
                        ContentValuesBuilder.values(1)
                                .put(Tables.Sounds.REMOVED_AT, dateProvider.currentTime)
                                .get(),
                        Filter.filter().whereEq(Tables.Sounds._ID, urn.numericId)
                                .whereEq(Tables.Sounds._TYPE, Tables.Sounds.TYPE_PLAYLIST)
                ))
            }

            private fun removePlaylistFromPostsTable(propeller: PropellerDatabase) {
                step<ChangeResult>(propeller.delete(Tables.Posts.TABLE, Filter.filter()
                        .whereEq(Tables.Posts.TARGET_TYPE, Tables.Sounds.TYPE_PLAYLIST)
                        .whereEq(Tables.Posts.TARGET_ID, urn.numericId)))
            }

            private fun removePlaylistFromActivitiesView(propeller: PropellerDatabase) {
                step<ChangeResult>(propeller.delete(Table.Activities, Filter.filter()
                        .whereEq(TableColumns.Activities.SOUND_TYPE, Tables.Sounds.TYPE_PLAYLIST)
                        .whereEq(TableColumns.Activities.SOUND_ID, urn.numericId)))

                step<ChangeResult>(propeller.delete(Table.SoundStream, Filter.filter()
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, Tables.Sounds.TYPE_PLAYLIST)
                        .whereEq(TableColumns.SoundStream.SOUND_ID, urn.numericId)))
            }

            private fun removePlaylistFromSoundStreamView(propeller: PropellerDatabase) {
                step<ChangeResult>(propeller.delete(Table.SoundStream, Filter.filter()
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, Tables.Sounds.TYPE_PLAYLIST)
                        .whereEq(TableColumns.SoundStream.SOUND_ID, urn.numericId)))
            }
        }).singleOrError()
    }

    fun removePlaylist(urn: Urn): Single<Boolean> {
        require(urn.isPlaylist) { "urn argument: $urn is not a playlist urn" }

        return removePlaylistCommand.toSingle(urn)
    }

    private fun playlistPostQuery(limit: Int, fromTimestamp: Long): Query {
        return Query.from(Tables.Posts.TABLE)
                .select(Tables.Posts.TARGET_ID, Tables.Posts.CREATED_AT)
                .whereNull(Tables.Posts.REMOVED_AT)
                .whereEq(Tables.Posts.TYPE, Tables.Posts.TYPE_POST)
                .whereEq(Tables.Posts.TARGET_TYPE, Tables.Sounds.TYPE_PLAYLIST)
                .whereLt(Tables.Posts.CREATED_AT, fromTimestamp)
                .order(Tables.Posts.CREATED_AT, Query.Order.DESC)
                .limit(limit)
    }

    private fun loadPostedTracksSortedByDateDescQuery(): Query {
        return Query.from(Tables.Posts.TABLE)
                .select(Tables.Posts.TARGET_ID, Tables.Posts.CREATED_AT)
                .whereEq(Tables.Posts.TYPE, Tables.Posts.TYPE_POST)
                .whereEq(Tables.Posts.TARGET_TYPE, Tables.Sounds.TYPE_TRACK)
                .order(Tables.Posts.CREATED_AT.qualifiedName(), Query.Order.DESC)
    }
}
