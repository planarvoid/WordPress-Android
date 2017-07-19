package com.soundcloud.android.olddiscovery.recommendedplaylists

import com.soundcloud.android.model.Urn
import com.soundcloud.java.optional.Optional

@Suppress("LongParameterList")
data class RecommendedPlaylistsEntity(
        val localId: Long,
        val key: String,
        val displayName: String,
        val artworkUrl: String?,
        val queryUrn: Urn?,
        val playlistUrns: List<Urn> = arrayListOf()
) {
    companion object {
        @JvmStatic
        fun create(localId: Long, key: String, displayName: String, artworkUrl: Optional<String>, queryUrn: Optional<Urn>, playlistUrns: List<Urn>): RecommendedPlaylistsEntity {
            return RecommendedPlaylistsEntity(localId, key, displayName, artworkUrl.orNull(), queryUrn.orNull(), playlistUrns)
        }

        @JvmStatic
        fun create(localId: Long, key: String, displayName: String, artworkUrl: Optional<String>, queryUrn: Optional<Urn>): RecommendedPlaylistsEntity {
            return RecommendedPlaylistsEntity(localId, key, displayName, artworkUrl.orNull(), queryUrn.orNull())
        }
    }

    fun getOptionalArtworkUrl(): Optional<String> = Optional.fromNullable(artworkUrl)
    fun getOptionalQueryUrn(): Optional<Urn> = Optional.fromNullable(queryUrn)

    fun copyWithPlaylistUrns(urns: List<Urn>) = copy(playlistUrns = urns)
}
