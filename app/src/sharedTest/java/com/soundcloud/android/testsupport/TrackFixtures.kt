package com.soundcloud.android.testsupport

import com.google.common.collect.Lists
import com.soundcloud.android.api.model.ApiTrack
import com.soundcloud.android.api.model.ApiTrackStats
import com.soundcloud.android.api.model.Sharing
import com.soundcloud.android.model.Urn
import com.soundcloud.android.offline.OfflineProperties
import com.soundcloud.android.presentation.EntityItemCreator
import com.soundcloud.android.stream.StreamEntity
import com.soundcloud.android.tracks.Track
import com.soundcloud.android.tracks.TrackItem
import com.soundcloud.java.optional.Optional
import java.util.Arrays
import java.util.Date

@Suppress("MemberVisibilityCanPrivate")
object TrackFixtures {

    fun entityItemCreator(): EntityItemCreator = EntityItemCreator(TestOfflinePropertiesProvider(OfflineProperties()))

    private var runningId = 1L

    @JvmField val SUB_MID_TIER: Optional<Boolean> = Optional.of(false)
    @JvmField val SUB_HIGH_TIER: Optional<Boolean> = Optional.of(false)
    @JvmField val IS_COMMENTABLE = false
    @JvmField val IS_MONETIZABLE = true
    @JvmField val IS_BLOCKED = false
    @JvmField val IS_SYNCABLE = true
    @JvmField val USER_TAGS = Arrays.asList("tag1", "tag2")
    @JvmField val SNIPPED = false
    @JvmField val MONETIZATION_MODEL = Optional.of("monetizable")
    @JvmField val DESCRIPTION = Optional.of("description")
    @JvmField val DISPLAY_STATS_ENABLED = true
    @JvmField val SNIPPET_DURATION = 12345L
    @JvmField val FULL_DURATION = 678910L
    @JvmField val WAVEFORM_URL = "http://waveform.url"
    @JvmField val SHARING = Sharing.PUBLIC
    @JvmField val GENRE = "Clownstep"
    @JvmField val POLICY = "allowed"
    @JvmField val PLAYBACK_COUNT = 10
    @JvmField val COMMENT_COUNT = 11
    @JvmField val REPOST_COUNT = 12
    @JvmField val LIKE_COUNT = 13

    private fun getTitle(trackUrn: Urn): String = "track " + trackUrn

    private fun artworkTemplateUrl(id: Long): String = "https://i1.sndcdn.com/artworks-$id-{size}.jpg"

    private fun permalinkUrl(id: Long): String = "https://soundcloud.com/user-" + id

    @JvmStatic
    fun apiTrack(): ApiTrack = apiTrack(nextUrn())

    private fun nextUrn() = Urn.forTrack(runningId++)

    @JvmStatic
    fun apiTrack(urn: Urn): ApiTrack = apiTrackBuilder(urn).build()

    @JvmStatic
    fun apiTrackBuilder() = apiTrackBuilder(nextUrn())

    @JvmStatic
    fun apiTrackBuilder(urn: Urn): ApiTrack.Builder {
        return ApiTrack.builder(urn)
                .title(getTitle(urn))
                .genre(GENRE)
                .user(UserFixtures.apiUser())
                .commentable(IS_COMMENTABLE)
                .snippetDuration(SNIPPET_DURATION)
                .fullDuration(FULL_DURATION)
                .waveformUrl(WAVEFORM_URL)
                .imageUrlTemplate(Optional.of(artworkTemplateUrl(urn.numericId)))
                .permalinkUrl(permalinkUrl(urn.numericId))
                .stats(ApiTrackStats.create(PLAYBACK_COUNT, COMMENT_COUNT, REPOST_COUNT, LIKE_COUNT))
                .userTags(USER_TAGS)
                .createdAt(Date())
                .sharing(SHARING)
                .monetizable(IS_MONETIZABLE)
                .blocked(IS_BLOCKED)
                .snipped(SNIPPED)
                .policy(POLICY)
                .monetizationModel(MONETIZATION_MODEL)
                .isSubMidTier(SUB_MID_TIER)
                .isSubHighTier(SUB_HIGH_TIER)
                .syncable(IS_SYNCABLE)
                .description(DESCRIPTION)
                .displayStatsEnabled(DISPLAY_STATS_ENABLED)
    }

    @JvmStatic
    fun trackBuilder(): Track.Builder = track().toBuilder()

    @JvmStatic
    fun trackItem(track: ApiTrack): TrackItem = trackItem(Track.from(track))

    @JvmStatic
    fun trackItem(track: Track): TrackItem = entityItemCreator().trackItem(track)

    @JvmStatic
    fun trackItem(track: Track, streamEntity: StreamEntity): TrackItem = entityItemCreator().trackItem(track, streamEntity)

    @JvmStatic
    fun trackItem(): TrackItem = TrackItem.builder(track(), OfflineProperties()).build()

    @JvmStatic
    fun trackItems(apiTracks: List<ApiTrack>): List<TrackItem> = apiTracks.map { trackItem(it) }

    @JvmStatic
    fun trackItemBuilder(): TrackItem.Builder = trackItem().toBuilder()

    @JvmStatic
    fun trackItemBuilder(urn: Urn): TrackItem.Builder = entityItemCreator().trackItem(trackBuilder().urn(urn).build()).toBuilder()

    @JvmStatic
    fun trackItemBuilder(apiTrack: ApiTrack): TrackItem.Builder = entityItemCreator().trackItem(trackBuilder(apiTrack).build()).toBuilder()

    @JvmStatic
    fun trackItem(urn: Urn): TrackItem = entityItemCreator().trackItem(trackBuilder().urn(urn).build())

    @JvmStatic
    fun track(): Track = track(apiTrack())

    @JvmStatic
    fun track(apiTrack: ApiTrack) = Track.from(apiTrack)

    fun trackBuilder(apiTrack: ApiTrack): Track.Builder = Track.from(apiTrack).toBuilder()

    @JvmStatic
    fun trackItems(count: Int) = apiTracks(count).map { trackItem(it) }

    @JvmStatic
    fun tracks(count: Int): List<Track> = Lists.transform(apiTracks(count), { track(it!!) })

    @JvmStatic
    fun apiTracks(count: Int) = List(count, { apiTrack() })

}
