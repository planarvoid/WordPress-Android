package com.soundcloud.android.stream

import com.soundcloud.android.ads.AdData
import com.soundcloud.android.ads.AppInstallAd
import com.soundcloud.android.ads.VideoAd
import com.soundcloud.android.events.CurrentPlayQueueItemEvent
import com.soundcloud.android.events.LikesStatusEvent
import com.soundcloud.android.events.RepostsStatusEvent
import com.soundcloud.android.model.Urn
import com.soundcloud.android.playlists.Playlist
import com.soundcloud.android.playlists.PlaylistItem
import com.soundcloud.android.presentation.LikeableItem
import com.soundcloud.android.presentation.PlayableItem
import com.soundcloud.android.presentation.RepostableItem
import com.soundcloud.android.presentation.UpdatablePlaylistItem
import com.soundcloud.android.presentation.UpdatableTrackItem
import com.soundcloud.android.suggestedcreators.SuggestedCreator
import com.soundcloud.android.suggestedcreators.SuggestedCreatorItem
import com.soundcloud.android.tracks.Track
import com.soundcloud.android.tracks.TrackItem
import com.soundcloud.android.view.adapters.PlayableViewItem
import com.soundcloud.java.optional.Optional
import java.util.Date

sealed class StreamItem(val kind: Kind) {

    internal val playableItem: Optional<PlayableItem>
        get() = when (this) {
            is TrackStreamItem -> Optional.of(this.trackItem)
            is PlaylistStreamItem -> Optional.of(this.playlistItem)
            else -> Optional.absent()
        }

    val adData: Optional<AdData>
        get() = when (this) {
            is AppInstall -> Optional.of(this.appInstallAd)
            is Video -> Optional.of(this.video)
            else -> Optional.absent()
        }

    val isPromoted: Boolean
        get() = when (this) {
            is TrackStreamItem -> this.promoted
            is PlaylistStreamItem -> this.promoted
            else -> false
        }
    val isAd: Boolean
        get() = this is AppInstall || this is Video

    val isUpsell: Boolean
        get() = this is Upsell

    enum class Kind {
        TRACK,
        PLAYLIST,
        FACEBOOK_LISTENER_INVITES,
        FACEBOOK_CREATORS,
        STREAM_UPSELL,
        SUGGESTED_CREATORS,
        APP_INSTALL,
        VIDEO_AD
    }

    data class FacebookCreatorInvites(val trackUrn: Urn, val trackUrl: String) : StreamItem(Kind.FACEBOOK_CREATORS)

    data class FacebookListenerInvites(val friendPictureUrls: Optional<List<String>> = Optional.absent()) : StreamItem(Kind.FACEBOOK_LISTENER_INVITES) {
        fun hasPictures(): Boolean {
            return friendPictureUrls.isPresent && !friendPictureUrls.get().isEmpty()
        }
    }

    data class SuggestedCreators(val suggestedCreators: List<SuggestedCreatorItem>) : StreamItem(Kind.SUGGESTED_CREATORS) {
        companion object {

            fun create(suggestedCreators: List<SuggestedCreator>): SuggestedCreators {
                return SuggestedCreators(suggestedCreators.map { SuggestedCreatorItem.fromSuggestedCreator(it) })
            }
        }
    }

    data class AppInstall(val appInstallAd: AppInstallAd) : StreamItem(Kind.APP_INSTALL)

    data class Video(val video: VideoAd) : StreamItem(Kind.VIDEO_AD)

    object Upsell : StreamItem(Kind.STREAM_UPSELL)
}

data class TrackStreamItem(val trackItem: TrackItem, val promoted: Boolean, val createdAt: Date, val avatarUrlTemplate: Optional<String>) : StreamItem(Kind.TRACK),
                                                                                                                                            PlayableViewItem<TrackStreamItem>,
                                                                                                                                            UpdatableTrackItem,
                                                                                                                                            LikeableItem,
                                                                                                                                            RepostableItem {

    override val urn: Urn
        get() = trackItem.urn

    override fun updatedWithTrack(track: Track): TrackStreamItem {
        return copy(trackItem = trackItem.updatedWithTrack(track))
    }

    override fun updatedWithLike(likeStatus: LikesStatusEvent.LikeStatus): TrackStreamItem {
        return copy(trackItem = trackItem.updatedWithLike(likeStatus))
    }

    override fun updatedWithRepost(repostStatus: RepostsStatusEvent.RepostStatus): TrackStreamItem {
        return copy(trackItem = trackItem.updatedWithRepost(repostStatus))
    }

    override fun updateNowPlaying(event: CurrentPlayQueueItemEvent): TrackStreamItem {
        val updatedTrackItem = trackItem.updateNowPlaying(event.currentPlayQueueItem.urnOrNotSet)
        return copy(trackItem = updatedTrackItem)
    }

    companion object {

        fun create(trackItem: TrackItem, createdAt: Date, avatarUrlTemplate: Optional<String>): TrackStreamItem {
            return TrackStreamItem(trackItem, trackItem.isPromoted, createdAt, avatarUrlTemplate)
        }
    }
}

data class PlaylistStreamItem(val playlistItem: PlaylistItem, val promoted: Boolean, val createdAt: Date, val avatarUrlTemplate: Optional<String>) : StreamItem(Kind.PLAYLIST),
                                                                                                                                                     LikeableItem,
                                                                                                                                                     RepostableItem,
                                                                                                                                                     UpdatablePlaylistItem {

    override val urn: Urn
        get() = playlistItem.urn

    override fun updatedWithLike(likeStatus: LikesStatusEvent.LikeStatus): PlaylistStreamItem {
        return copy(playlistItem = playlistItem.updatedWithLike(likeStatus))
    }

    override fun updatedWithRepost(repostStatus: RepostsStatusEvent.RepostStatus): PlaylistStreamItem {
        return copy(playlistItem = playlistItem.updatedWithRepost(repostStatus))
    }

    override fun updatedWithPlaylist(playlist: Playlist): PlaylistStreamItem {
        return copy(playlistItem = playlistItem.toBuilder().playlist(playlist).build())
    }

    override fun updatedWithTrackCount(trackCount: Int): PlaylistStreamItem {
        return copy(playlistItem = playlistItem.updatedWithTrackCount(trackCount))
    }

    override fun updatedWithMarkedForOffline(markedForOffline: Boolean): PlaylistStreamItem {
        return copy(playlistItem = playlistItem.updatedWithMarkedForOffline(markedForOffline))
    }

    companion object {

        internal fun create(playlistItem: PlaylistItem,
                            createdAt: Date, avatarUrlTemplate: Optional<String>): PlaylistStreamItem {
            return PlaylistStreamItem(playlistItem, playlistItem.isPromoted, createdAt, avatarUrlTemplate)
        }
    }
}
