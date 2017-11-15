package com.soundcloud.android.tracks

import com.soundcloud.android.associations.RepostsStateProvider
import com.soundcloud.android.likes.LikesStateProvider
import com.soundcloud.android.model.Urn
import com.soundcloud.android.offline.IOfflinePropertiesProvider
import com.soundcloud.android.playback.PlaySessionStateProvider
import com.soundcloud.android.presentation.EntityItemCreator
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.java.collections.Iterables
import com.soundcloud.java.collections.Lists
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Observables
import javax.inject.Inject

@OpenForTesting
class TrackItemRepository
@Inject
constructor(private val trackRepository: TrackRepository,
            private val entityItemCreator: EntityItemCreator,
            private val likesStateProvider: LikesStateProvider,
            private val repostsStateProvider: RepostsStateProvider,
            private val playSessionStateProvider: PlaySessionStateProvider,
            private val offlinePropertiesProvider: IOfflinePropertiesProvider) {

    fun track(trackUrn: Urn): Maybe<TrackItem> {
        return trackRepository.track(trackUrn).map { entityItemCreator.trackItem(it) }
    }

    fun liveTrack(trackUrn: Urn): Observable<TrackItem> {
        return liveFromUrns(listOf(trackUrn))
                .filter { urnTrackMap -> urnTrackMap.containsKey(trackUrn) }
                .map { urnTrackMap -> urnTrackMap[trackUrn] }
    }

    fun fromUrns(requestedTracks: List<Urn>): Single<Map<Urn, TrackItem>> {
        return trackRepository.fromUrns(requestedTracks).map { entityItemCreator.convertTrackMap(it) }
    }

    fun liveFromUrns(requestedTracks: List<Urn>): Observable<Map<Urn, TrackItem>> {
        return Observables.combineLatest(trackRepository.liveFromUrns(requestedTracks),
                                         likesStateProvider.likedStatuses(),
                                         repostsStateProvider.repostedStatuses(),
                                         playSessionStateProvider.nowPlayingUrn(),
                                         offlinePropertiesProvider.states(),
                                         { tracks, likes, reposts, nowPlayingUrn, offlineStates ->
                                             tracks.mapValues {
                                                 TrackItem.from(it.value,
                                                                offlineStates,
                                                                likes,
                                                                reposts,
                                                                nowPlayingUrn)
                                             }
                                         }).distinctUntilChanged()
    }

    fun trackListFromUrns(requestedTracks: List<Urn>): Single<List<TrackItem>> {
        return fromUrns(requestedTracks)
                .map { urnTrackMap ->
                    Lists.newArrayList(Iterables.transform<Urn, TrackItem>(Iterables.filter<Urn>(requestedTracks, { urnTrackMap.containsKey(it) }),
                                                                           { urnTrackMap[it] }))
                }
    }

    fun forPlaylist(playlistUrn: Urn): Single<List<TrackItem>> {
        return trackRepository.forPlaylist(playlistUrn).map { t -> Lists.transform<Track, TrackItem>(t, { entityItemCreator.trackItem(it) }) }

    }

    fun forPlaylist(playlistUrn: Urn, staleTimeMillis: Long): Single<List<TrackItem>> {
        return trackRepository.forPlaylist(playlistUrn, staleTimeMillis).map { t -> Lists.transform<Track, TrackItem>(t, { entityItemCreator.trackItem(it) }) }
    }

    fun fullTrackWithUpdate(trackUrn: Urn): Observable<TrackItem> {
        return trackRepository.fullTrackWithUpdate(trackUrn).map { entityItemCreator.trackItem(it) }
    }

}
