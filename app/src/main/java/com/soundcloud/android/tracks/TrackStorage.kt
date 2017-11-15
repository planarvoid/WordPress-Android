package com.soundcloud.android.tracks

import android.provider.BaseColumns
import android.provider.BaseColumns._ID
import com.soundcloud.android.Consts
import com.soundcloud.android.commands.StoreTracksCommand
import com.soundcloud.android.commands.TrackUrnMapperV2
import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.Table
import com.soundcloud.android.storage.TableColumns
import com.soundcloud.android.storage.TableColumns.ResourceTable._TYPE
import com.soundcloud.android.storage.Tables
import com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK
import com.soundcloud.android.storage.Tables.TrackView.ARTWORK_URL
import com.soundcloud.android.storage.Tables.TrackView.BLOCKED
import com.soundcloud.android.storage.Tables.TrackView.COMMENTS_COUNT
import com.soundcloud.android.storage.Tables.TrackView.CREATED_AT
import com.soundcloud.android.storage.Tables.TrackView.CREATOR_ID
import com.soundcloud.android.storage.Tables.TrackView.CREATOR_IS_PRO
import com.soundcloud.android.storage.Tables.TrackView.CREATOR_NAME
import com.soundcloud.android.storage.Tables.TrackView.FULL_DURATION
import com.soundcloud.android.storage.Tables.TrackView.ID
import com.soundcloud.android.storage.Tables.TrackView.IS_COMMENTABLE
import com.soundcloud.android.storage.Tables.TrackView.IS_USER_LIKE
import com.soundcloud.android.storage.Tables.TrackView.IS_USER_REPOST
import com.soundcloud.android.storage.Tables.TrackView.LIKES_COUNT
import com.soundcloud.android.storage.Tables.TrackView.MONETIZABLE
import com.soundcloud.android.storage.Tables.TrackView.MONETIZATION_MODEL
import com.soundcloud.android.storage.Tables.TrackView.PERMALINK_URL
import com.soundcloud.android.storage.Tables.TrackView.PLAY_COUNT
import com.soundcloud.android.storage.Tables.TrackView.POLICY
import com.soundcloud.android.storage.Tables.TrackView.POLICY_LAST_UPDATED_AT
import com.soundcloud.android.storage.Tables.TrackView.REPOSTS_COUNT
import com.soundcloud.android.storage.Tables.TrackView.SHARING
import com.soundcloud.android.storage.Tables.TrackView.SNIPPED
import com.soundcloud.android.storage.Tables.TrackView.SNIPPET_DURATION
import com.soundcloud.android.storage.Tables.TrackView.SUB_HIGH_TIER
import com.soundcloud.android.storage.Tables.TrackView.SUB_MID_TIER
import com.soundcloud.android.storage.Tables.TrackView.SYNCABLE
import com.soundcloud.android.storage.Tables.TrackView.TITLE
import com.soundcloud.android.storage.Tables.TrackView.WAVEFORM_URL
import com.soundcloud.android.utils.ErrorUtils
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.android.utils.Urns
import com.soundcloud.java.collections.Lists.partition
import com.soundcloud.java.collections.MoreCollections.transform
import com.soundcloud.java.optional.Optional
import com.soundcloud.java.strings.Strings
import com.soundcloud.propeller.CursorReader
import com.soundcloud.propeller.QueryResult
import com.soundcloud.propeller.WriteResult
import com.soundcloud.propeller.query.Query
import com.soundcloud.propeller.rx.PropellerRxV2
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Function
import io.reactivex.subjects.BehaviorSubject
import java.lang.String.format
import java.util.Date
import javax.inject.Inject

@OpenForTesting
class TrackStorage
@Inject
constructor(private val propeller: PropellerRxV2, private val storeTracksCommand: StoreTracksCommand) {

    private val trackChangeSubject = BehaviorSubject.createDefault(emptyList<Urn>())

    private val fetchAvailableTrackUrns = Function<List<Urn>, Observable<Urn>> { urns ->
        propeller.queryResult(buildAvailableTracksQuery(urns))
                .flatMap { result -> Observable.fromIterable(result.toList(TRACK_URN_MAPPER)) }
    }

    fun urnForPermalink(permalink: String): Maybe<Urn> {
        if (permalink.startsWith("/")) {
            throw IllegalArgumentException("Permalink must not start with a '/' and must not be a url.")
        }
        return propeller.queryResult(buildPermalinkQuery(permalink))
                .filter { queryResult -> !queryResult.isEmpty }
                .map { queryResult -> queryResult.first { cursorReader -> Urn.forTrack(cursorReader.getLong(Tables.TrackView.ID)) } }
                .firstElement()
    }

    fun loadTrack(urn: Urn): Maybe<Track> {
        return propeller.queryResult(buildTrackQuery(urn))
                .map { result -> result.toList { trackFromCursorReader(it) }.filterNotNull() }
                .flatMap { Observable.fromIterable(it) }
                .firstElement()
    }

    fun loadTracks(urns: List<Urn>): Single<Map<Urn, Track>> {
        return batchedTracks(urns).toList().map { this.toMapOfUrnAndTrack(it) }
    }

    private fun batchedTracks(urns: List<Urn>): Observable<QueryResult> {
        return Observable.fromIterable(partition(urns, MAX_TRACKS_BATCH)).flatMap { propeller.queryResult(buildTracksQuery(it)) }
    }

    private fun batchedAvailableTracks(urns: List<Urn>): Observable<Urn> {
        return Observable.fromIterable(partition(urns, MAX_TRACKS_BATCH)).flatMap(fetchAvailableTrackUrns)
    }

    private fun toMapOfUrnAndTrack(cursorReadersBatches: List<QueryResult>): Map<Urn, Track> {
        val tracks = mutableMapOf<Urn, Track>()
        for (cursorReaders in cursorReadersBatches) {
            for (cursorReader in cursorReaders) {
                val track = trackFromCursorReader(cursorReader)
                track?.let { tracks.put(it.urn(), it) }
            }
        }
        return tracks
    }

    fun availableTracks(requestedTracks: List<Urn>): Observable<List<Urn>> {
        return changedTracks(requestedTracks)
                .flatMapSingle { batchedAvailableTracks(requestedTracks).toList() }
    }

    private fun changedTracks(requestedTracks: List<Urn>): Observable<Set<Urn>> {
        return trackChangeSubject
                .map { changedTracks -> changedTracks.intersect(requestedTracks) }
                .distinctUntilChanged()
    }

    fun loadTrackDescription(urn: Urn): Single<Optional<String>> {
        return propeller.queryResult(buildTrackDescriptionQuery(urn))
                .map { result -> result.toList(TrackDescriptionMapper()) }
                .flatMap { Observable.fromIterable(it) }
                .first(Optional.absent())
    }

    fun storeTracks(tracks: Iterable<TrackRecord>): WriteResult {
        val writeResult = storeTracksCommand.call(tracks)
        tracksChanged(writeResult, tracks)
        return writeResult
    }

    fun asyncStoreTracks(tracks: Iterable<TrackRecord>): Single<WriteResult> {
        return storeTracksCommand.toSingle(tracks).doOnSuccess { tracksChanged(it, tracks) }
    }

    private fun tracksChanged(writeResult: WriteResult, tracks: Iterable<TrackRecord>) {
        if (writeResult.success()) {
            trackChangeSubject.onNext(tracks.map { it.urn })
        }
    }

    private fun buildTrackDescriptionQuery(trackUrn: Urn): Query {
        return Query.from(Table.SoundView.name)
                .select(TableColumns.SoundView.DESCRIPTION)
                .whereEq(_ID, trackUrn.numericId)
                .whereEq(_TYPE, TYPE_TRACK)
    }

    private fun buildTrackQuery(trackUrn: Urn): Query {
        return Query.from(Tables.TrackView.TABLE)
                .select("*")
                .whereEq(Tables.TrackView.ID.name(), trackUrn.numericId)
    }

    private fun buildTracksQuery(trackUrns: List<Urn>): Query {
        return Query.from(Tables.TrackView.TABLE)
                .select("*")
                .whereIn(Tables.TrackView.ID.name(), transform(trackUrns, Urns.TO_ID))
    }

    private fun buildAvailableTracksQuery(trackUrns: List<Urn>): Query {
        return Query.from(Tables.Sounds.TABLE)
                .select(Tables.Sounds._ID.`as`(BaseColumns._ID))
                .whereEq(Tables.Sounds._TYPE, TYPE_TRACK)
                .whereIn(Tables.Sounds._ID, transform(trackUrns, Urns.TO_ID))
    }

    private fun buildPermalinkQuery(permalink: String): Query {
        return Query.from(Tables.TrackView.TABLE)
                .select(Tables.TrackView.ID)
                .whereIn(Tables.TrackView.PERMALINK_URL, "https://soundcloud.com/" + permalink)
                .limit(1)
    }

    companion object {

        private val MAX_TRACKS_BATCH = 200
        private val SHARING_PRIVATE = "private"
        private val TRACK_URN_MAPPER = TrackUrnMapperV2()

        fun optionalTrackFromCursorReader(cursorReader: CursorReader): Optional<Track> {
            return Optional.fromNullable(trackFromCursorReader(cursorReader))
        }

        private fun trackFromCursorReader(cursorReader: CursorReader): Track? {
            val builder = Track.builder()
            builder.urn(Urn.forTrack(cursorReader.getLong(ID.name())))
            builder.title(cursorReader.getString(TITLE.name()))
            builder.snippetDuration(cursorReader.getLong(SNIPPET_DURATION.name()))
            builder.fullDuration(cursorReader.getLong(FULL_DURATION.name()))
            builder.playCount(cursorReader.getInt(PLAY_COUNT.name()))
            builder.commentsCount(cursorReader.getInt(COMMENTS_COUNT.name()))
            builder.commentable(cursorReader.getBoolean(IS_COMMENTABLE.name()))
            builder.likesCount(cursorReader.getInt(LIKES_COUNT.name()))
            builder.repostsCount(cursorReader.getInt(REPOSTS_COUNT.name()))
            builder.monetizable(cursorReader.getBoolean(MONETIZABLE.name()))
            builder.blocked(cursorReader.getBoolean(BLOCKED.name()))
            builder.isSyncable(cursorReader.getBoolean(SYNCABLE.name()))
            builder.snipped(cursorReader.getBoolean(SNIPPED.name()))
            builder.subHighTier(cursorReader.getBoolean(SUB_HIGH_TIER.name()))
            builder.subMidTier(cursorReader.getBoolean(SUB_MID_TIER.name()))
            builder.monetizationModel(Optional.fromNullable(cursorReader.getString(MONETIZATION_MODEL.name())).or(Strings.EMPTY))
            builder.userLike(cursorReader.getBoolean(IS_USER_LIKE.name()))
            builder.permalinkUrl(cursorReader.getString(PERMALINK_URL.name()))
            builder.userRepost(cursorReader.getBoolean(IS_USER_REPOST.name()))
            builder.isPrivate(SHARING_PRIVATE.equals(cursorReader.getString(SHARING.name()), ignoreCase = true))
            builder.createdAt(cursorReader.getDateFromTimestamp(CREATED_AT.name()))
            builder.imageUrlTemplate(Optional.fromNullable(cursorReader.getString(ARTWORK_URL.name())))
            builder.creatorIsPro(cursorReader.getBoolean(CREATOR_IS_PRO.name()))

            builder.genre(Optional.fromNullable(cursorReader.getString(Tables.TrackView.GENRE.name())))
            builder.displayStatsEnabled(cursorReader.getBoolean(Tables.TrackView.DISPLAY_STATS_ENABLED.name()))
            builder.secretToken(Optional.fromNullable(cursorReader.getString(Tables.TrackView.SECRET_TOKEN.name())))

            builder.policyLastUpdatedAt(getDateOr(cursorReader, POLICY_LAST_UPDATED_AT.name(), Date(0)))
            builder.waveformUrl(Optional.fromNullable(cursorReader.getString(WAVEFORM_URL.name())).or(Strings.EMPTY))

            // synced tracks that might not have a user if they haven't been lazily updated yet
            builder.creatorName(Optional.fromNullable(cursorReader.getString(CREATOR_NAME.name())).or(Strings.EMPTY))
            val creatorId = cursorReader.getLong(CREATOR_ID.name())
            builder.creatorUrn(if (creatorId == Consts.NOT_SET.toLong()) Urn.NOT_SET else Urn.forUser(creatorId))

            builder.description(Optional.absent())
            val policy = cursorReader.getString(POLICY.name())
            return if (policy == null) {
                ErrorUtils.handleSilentException(IllegalStateException(format("Track found without policy: %s", builder.policy("<missing>").build())))
                null
            } else {
                builder.policy(policy).build()
            }
        }

        private fun getDateOr(cursorReader: CursorReader, columnName: String, defaultDate: Date): Date {
            return if (cursorReader.isNotNull(columnName)) {
                cursorReader.getDateFromTimestamp(columnName)
            } else defaultDate
        }
    }
}
