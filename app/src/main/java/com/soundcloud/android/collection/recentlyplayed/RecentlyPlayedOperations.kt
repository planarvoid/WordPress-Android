package com.soundcloud.android.collection.recentlyplayed

import com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY
import com.soundcloud.android.collection.playhistory.PlayHistoryRecord
import com.soundcloud.android.model.Urn
import com.soundcloud.android.playlists.Playlist
import com.soundcloud.android.playlists.PlaylistRepository
import com.soundcloud.android.stations.StationMetadata
import com.soundcloud.android.stations.StationsRepository
import com.soundcloud.android.sync.NewSyncOperations
import com.soundcloud.android.sync.SyncResult
import com.soundcloud.android.sync.Syncable
import com.soundcloud.android.users.User
import com.soundcloud.android.users.UserRepository
import com.soundcloud.android.utils.ErrorUtils
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.functions.Function3
import javax.inject.Inject
import javax.inject.Named

typealias RecentlyPlayedPlayableItemsLoader = (List<Urn>, Map<Urn, Long>) -> Single<RecentlyPlayedPlayableItems>
typealias RecentlyPlayedPlayableItems = List<RecentlyPlayedPlayableItem>

open class RecentlyPlayedOperations
@Inject
constructor(private val recentlyPlayedStorage: RecentlyPlayedStorage,
            @param:Named(RX_HIGH_PRIORITY) private val scheduler: Scheduler,
            private val syncOperations: NewSyncOperations,
            private val clearRecentlyPlayedCommand: ClearRecentlyPlayedCommand,
            private val userRepository: UserRepository,
            private val playlistRepository: PlaylistRepository,
            private val stationsRepository: StationsRepository) {

    companion object {
        const val CAROUSEL_ITEMS = 10
        const val MAX_RECENTLY_PLAYED = 1000
    }

    @JvmOverloads
    open fun recentlyPlayed(limit: Int = MAX_RECENTLY_PLAYED): Single<List<RecentlyPlayedPlayableItem>> {
        return syncOperations.lazySyncIfStale(Syncable.RECENTLY_PLAYED)
                .observeOn(scheduler)
                .onErrorResumeNext(Single.just(SyncResult.noOp()))
                .flatMap { recentlyPlayedItems(limit) }
    }

    @JvmOverloads
    open fun refreshRecentlyPlayed(limit: Int = MAX_RECENTLY_PLAYED): Single<List<RecentlyPlayedPlayableItem>> {
        return syncOperations.failSafeSync(Syncable.RECENTLY_PLAYED)
                .observeOn(scheduler)
                .flatMap { recentlyPlayedItems(limit) }
    }

    open fun clearHistory(): Completable {
        return Completable.fromCallable { clearRecentlyPlayedCommand.call(null) }
                .subscribeOn(scheduler)
    }

    private fun recentlyPlayedItems(limit: Int): Single<List<RecentlyPlayedPlayableItem>> {
        return recentlyPlayedStorage.loadRecentlyPlayed(limit)
                .flatMap(this::resolveRecentlyPlayedMetadata)
    }

    private fun resolveRecentlyPlayedMetadata(playHistoryRecords: List<PlayHistoryRecord>): Single<RecentlyPlayedPlayableItems> {
        val recentlyPlayedPlaylists = playHistoryRecords.filter { it.isPlaylist }
        val recentlyPlayedArtists = playHistoryRecords.filter { it.isArtist() }
        val recentlyPlayedStations = playHistoryRecords.filter { it.isTrackStation() || it.isArtistStation() }

        val playlistItems = loadIfNotEmpty(recentlyPlayedPlaylists, this::loadPlaylists)
        val artistsItems = loadIfNotEmpty(recentlyPlayedArtists, this::loadArtists)
        val stationsItems = loadIfNotEmpty(recentlyPlayedStations, this::loadStations)

        return Single.zip(playlistItems, artistsItems, stationsItems, Function3(this::combineResults))
    }

    private fun combineResults(playlists: RecentlyPlayedPlayableItems, artists: RecentlyPlayedPlayableItems, stations: RecentlyPlayedPlayableItems): RecentlyPlayedPlayableItems {
        return (playlists + artists + stations)
                .sortedByDescending { it.timestamp }
                .toList()
    }

    private fun loadStations(urns: List<Urn>, timestamps: Map<Urn, Long>): Single<RecentlyPlayedPlayableItems> {
        return stationsRepository.stationsMetadata(urns)
                .flatMapObservable { Observable.fromIterable(it) }
                .map { it.toRecentlyPlayedPlayableItem(timestamps.getValue(it.urn())) }
                .toList()
    }

    private fun loadArtists(urns: List<Urn>, timestamps: Map<Urn, Long>): Single<RecentlyPlayedPlayableItems> {
        return userRepository.usersInfo(urns)
                .flatMapObservable { Observable.fromIterable(it) }
                .map { it.toRecentlyPlayedPlayableItem(timestamps.getValue(it.urn())) }
                .toList()
    }

    private fun loadPlaylists(urns: List<Urn>, timestamps: Map<Urn, Long>): Single<RecentlyPlayedPlayableItems> {
        return playlistRepository.loadPlaylistsByUrn(urns)
                .flatMapObservable { Observable.fromIterable(it) }
                .map { it.toRecentlyPlayedPlayableItem(timestamps.getValue(it.urn())) }
                .toList()
    }

    private fun loadIfNotEmpty(items: List<PlayHistoryRecord>, loader: RecentlyPlayedPlayableItemsLoader): Single<RecentlyPlayedPlayableItems> {
        if (items.isNotEmpty()) {
            val timestamps = items.associateBy(PlayHistoryRecord::contextUrn, PlayHistoryRecord::timestamp)
            val urns = items.map { it.contextUrn() }
            return loader(urns, timestamps)
                    .doOnError { ErrorUtils.handleSilentException(it) }
                    .onErrorReturnItem(listOf())
        }
        return Single.just(listOf<RecentlyPlayedPlayableItem>())
    }

    private fun Playlist.toRecentlyPlayedPlayableItem(timestamp: Long) =
            RecentlyPlayedPlayableItem.forPlaylist(urn(),
                    imageUrlTemplate(),
                    title(),
                    trackCount(),
                    isAlbum,
                    offlineState(),
                    isLikedByCurrentUser.or(false),
                    isPrivate,
                    timestamp
            )

    private fun User.toRecentlyPlayedPlayableItem(timestamp: Long) =
            RecentlyPlayedPlayableItem.forUser(urn(), username(), avatarUrl(), timestamp)

    private fun StationMetadata.toRecentlyPlayedPlayableItem(timestamp: Long) =
            RecentlyPlayedPlayableItem.forStation(urn(), title(), imageUrlTemplate(), timestamp)
}
