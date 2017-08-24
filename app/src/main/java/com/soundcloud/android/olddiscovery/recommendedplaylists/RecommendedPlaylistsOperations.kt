package com.soundcloud.android.olddiscovery.recommendedplaylists

import com.soundcloud.android.model.Urn
import com.soundcloud.android.olddiscovery.OldDiscoveryItem
import com.soundcloud.android.playlists.Playlist
import com.soundcloud.android.playlists.PlaylistRepository
import com.soundcloud.android.presentation.EntityItemCreator
import com.soundcloud.android.sync.NewSyncOperations
import com.soundcloud.android.sync.Syncable
import com.soundcloud.android.utils.OpenForTesting
import io.reactivex.Observable
import io.reactivex.functions.Function
import javax.inject.Inject

@OpenForTesting
class RecommendedPlaylistsOperations
@Inject
internal constructor(private val syncOperations: NewSyncOperations,
                     private val storage: RecommendedPlaylistsStorage,
                     private val playlistRepository: PlaylistRepository,
                     private val entityItemCreator: EntityItemCreator) {

    fun recommendedPlaylists(): Observable<OldDiscoveryItem> {
        return syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_PLAYLISTS)
                .flatMapObservable { _ -> readRecommendedPlaylistsFromStorage() }

    }

    fun refreshRecommendedPlaylists(): Observable<OldDiscoveryItem> {
        return syncOperations.failSafeSync(Syncable.RECOMMENDED_PLAYLISTS)
                .flatMapObservable { _ -> readRecommendedPlaylistsFromStorage() }
    }

    private fun readRecommendedPlaylistsFromStorage(): Observable<OldDiscoveryItem> {
        return storage.recommendedPlaylists()
                .toObservable()
                .flatMap { this.fromEntities(it) }
    }

    private fun fromEntities(buckets: List<RecommendedPlaylistsEntity>): Observable<RecommendedPlaylistsBucketItem> {
        val recommendedPlaylists = buckets.flatMap{ it.playlistUrns }

        if (recommendedPlaylists.isEmpty()) {
            return Observable.empty()
        }

        return playlistRepository.withUrns(recommendedPlaylists)
                .flatMapObservable(toOrderedBuckets(buckets))
    }

    private fun toOrderedBuckets(buckets: List<RecommendedPlaylistsEntity>): Function<Map<Urn, Playlist>, Observable<out RecommendedPlaylistsBucketItem>> {
        return Function {
            Observable
                    .fromIterable(buckets)
                    .map(mapToBucketItem(it))
        }
    }

    private fun mapToBucketItem(playlistEntities: Map<Urn, Playlist>): Function<RecommendedPlaylistsEntity, RecommendedPlaylistsBucketItem> {
        return Function {
            val matches = it.playlistUrns.map { entityItemCreator.playlistItem(playlistEntities[it]) }
            RecommendedPlaylistsBucketItem.create(it, matches)
        }
    }
}
