package com.soundcloud.android.playback.playqueue

import com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY
import com.soundcloud.android.model.Urn
import com.soundcloud.android.playback.PlayQueueManager
import com.soundcloud.android.playback.PlayQueueStorage
import com.soundcloud.android.playback.TrackQueueItem
import com.soundcloud.android.playlists.PlaylistRepository
import com.soundcloud.android.stations.StationsRepository
import com.soundcloud.android.tracks.TrackItem
import com.soundcloud.android.tracks.TrackItemRepository
import com.soundcloud.android.tracks.TrackRepository
import com.soundcloud.android.users.UserRepository
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.functions.Function4
import javax.inject.Inject
import javax.inject.Named

open class PlayQueueOperations
@Inject
constructor(@param:Named(RX_HIGH_PRIORITY) private val scheduler: Scheduler,
            private val playQueueManager: PlayQueueManager,
            private val trackItemRepository: TrackItemRepository,
            private val playQueueStorage: PlayQueueStorage,
            private val userRepository: UserRepository,
            private val stationsRepository: StationsRepository,
            private val playlistRepository: PlaylistRepository,
            private val trackRepository: TrackRepository) {

    open val tracks: Single<List<TrackAndPlayQueueItem>>
        get() = Single.defer { this.loadTracks() }

    open val contextTitles: Single<Map<Urn, String>>
        get() {
            val contextUrns = playQueueStorage.contextUrns

            val stations = getStations(contextUrns) { it.isStation }
            val users = getUsers(contextUrns) { it.isUser }
            val playlists = getPlaylists(contextUrns) { it.isPlaylist }
            val tracks = getTracks(contextUrns) { it.isTrack }

            return Single.zip<Map<Urn, String>, Map<Urn, String>, Map<Urn, String>, Map<Urn, String>, Map<Urn, String>>(users,
                    stations,
                    playlists,
                    tracks,
                    Function4 { t1, t2, t3, t4 -> t1.plus(t2).plus(t3).plus(t4) }).subscribeOn(scheduler)
        }

    private fun getStations(contextUrns: MutableList<Urn>, function: (Urn) -> Boolean): Single<Map<Urn, String>> {
        if (contextUrns.filter(function).isNotEmpty()) {
            return stationsRepository
                    .stationsMetadata(contextUrns.filter(function))
                    .map<Map<Urn, String>> { it.associateBy({ it.urn() }, { it.title() }) }

        } else {
            return Single.just(emptyMap())
        }
    }

    private fun getUsers(contextUrns: MutableList<Urn>, function: (Urn) -> Boolean): Single<Map<Urn, String>> {
        if (contextUrns.filter(function).isNotEmpty()) {
            return userRepository
                    .usersInfo(contextUrns.filter(function))
                    .map<Map<Urn, String>> { it.associateBy({ it.urn() }, { it.username() }) }

        } else {
            return Single.just(emptyMap())
        }
    }

    private fun getPlaylists(contextUrns: MutableList<Urn>, function: (Urn) -> Boolean): Single<Map<Urn, String>>? {
        if (contextUrns.filter(function).isNotEmpty()) {
            return playlistRepository
                    .withUrns(contextUrns.filter(function))
                    .map<Map<Urn, String>> { it.entries.associate { it.key to it.value.title() } }

        } else {
            return Single.just(emptyMap())
        }
    }

    private fun getTracks(contextUrns: MutableList<Urn>, function: (Urn) -> Boolean): Single<Map<Urn, String>>? {
        if (contextUrns.filter(function).isNotEmpty()) {
            return trackRepository
                    .fromUrns(contextUrns.filter(function))
                    .map<Map<Urn, String>> { it.entries.associate { it.key to it.value.title() } }

        } else {
            return Single.just(emptyMap())
        }
    }

    private fun loadTracks(): Single<List<TrackAndPlayQueueItem>> {
        val playQueueItems = playQueueManager
                .getPlayQueueItems { it != null && it.urn.isTrack }
                .map { it as TrackQueueItem }

        val uniqueTrackUrns = playQueueItems
                .map { it.urn }
                .distinct()

        return trackItemRepository
                .fromUrns(uniqueTrackUrns)
                .map { toTrackAndPlayQueueItem(playQueueItems, it) }
                .subscribeOn(scheduler)
    }

    private fun toTrackAndPlayQueueItem(playQueueItems: List<TrackQueueItem>,
                                        knownProperties: Map<Urn, TrackItem>): List<TrackAndPlayQueueItem> {
        return playQueueItems
                .filter { knownProperties.containsKey(it.urn) }
                .map { TrackAndPlayQueueItem(knownProperties[it.urn], it) }
    }

}
