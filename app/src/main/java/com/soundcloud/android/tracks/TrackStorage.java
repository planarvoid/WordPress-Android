package com.soundcloud.android.tracks;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.offline.OfflineStateMapper.getOfflineState;
import static com.soundcloud.android.storage.TableColumns.ResourceTable._TYPE;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK;
import static com.soundcloud.android.storage.Tables.TrackView.ARTWORK_URL;
import static com.soundcloud.android.storage.Tables.TrackView.BLOCKED;
import static com.soundcloud.android.storage.Tables.TrackView.COMMENTS_COUNT;
import static com.soundcloud.android.storage.Tables.TrackView.CREATED_AT;
import static com.soundcloud.android.storage.Tables.TrackView.CREATOR_ID;
import static com.soundcloud.android.storage.Tables.TrackView.CREATOR_NAME;
import static com.soundcloud.android.storage.Tables.TrackView.FULL_DURATION;
import static com.soundcloud.android.storage.Tables.TrackView.ID;
import static com.soundcloud.android.storage.Tables.TrackView.IS_COMMENTABLE;
import static com.soundcloud.android.storage.Tables.TrackView.IS_USER_LIKE;
import static com.soundcloud.android.storage.Tables.TrackView.IS_USER_REPOST;
import static com.soundcloud.android.storage.Tables.TrackView.LIKES_COUNT;
import static com.soundcloud.android.storage.Tables.TrackView.MONETIZABLE;
import static com.soundcloud.android.storage.Tables.TrackView.MONETIZATION_MODEL;
import static com.soundcloud.android.storage.Tables.TrackView.OFFLINE_DOWNLOADED_AT;
import static com.soundcloud.android.storage.Tables.TrackView.OFFLINE_REMOVED_AT;
import static com.soundcloud.android.storage.Tables.TrackView.OFFLINE_REQUESTED_AT;
import static com.soundcloud.android.storage.Tables.TrackView.OFFLINE_UNAVAILABLE_AT;
import static com.soundcloud.android.storage.Tables.TrackView.PERMALINK_URL;
import static com.soundcloud.android.storage.Tables.TrackView.PLAY_COUNT;
import static com.soundcloud.android.storage.Tables.TrackView.POLICY;
import static com.soundcloud.android.storage.Tables.TrackView.REPOSTS_COUNT;
import static com.soundcloud.android.storage.Tables.TrackView.SHARING;
import static com.soundcloud.android.storage.Tables.TrackView.SNIPPED;
import static com.soundcloud.android.storage.Tables.TrackView.SNIPPET_DURATION;
import static com.soundcloud.android.storage.Tables.TrackView.SUB_HIGH_TIER;
import static com.soundcloud.android.storage.Tables.TrackView.SUB_MID_TIER;
import static com.soundcloud.android.storage.Tables.TrackView.TITLE;
import static com.soundcloud.android.storage.Tables.TrackView.WAVEFORM_URL;
import static com.soundcloud.java.collections.Lists.partition;
import static com.soundcloud.java.collections.MoreCollections.transform;

import com.soundcloud.android.Consts;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;
import rx.functions.Func1;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TrackStorage {

    private static final int MAX_TRACKS_BATCH = 200;
    private static final String SHARING_PRIVATE = "private";

    private final PropellerRx propeller;

    private Func1<List<Urn>, Observable<QueryResult>> fetchTracks = new Func1<List<Urn>, Observable<QueryResult>>() {
        @Override
        public Observable<QueryResult> call(List<Urn> urns) {
            return propeller.queryResult(buildTracksQuery(urns));
        }
    };

    private Func1<List<Urn>, Observable<Urn>> fetchAvailableTrackUrns = new Func1<List<Urn>, Observable<Urn>>() {
        @Override
        public Observable<Urn> call(List<Urn> urns) {
            return propeller.query(buildAvailableTracksQuery(urns))
                            .map(new TrackUrnMapper());
        }
    };

    @Inject
    TrackStorage(PropellerRx propeller) {
        this.propeller = propeller;
    }

    Observable<Optional<Track>> loadTrack(Urn urn) {
        return propeller.query(buildTrackQuery(urn))
                        .map(TrackStorage::from)
                        .map(Optional::of)
                        .firstOrDefault(Optional.absent());
    }

    Observable<Map<Urn, Track>> loadTracks(List<Urn> urns) {
        return batchedTracks(urns).toList()
                                  .map(this::toMapOfUrnAndTrack)
                                  .firstOrDefault(Collections.emptyMap());
    }

    private Observable<QueryResult> batchedTracks(List<Urn> urns) {
        return Observable.from(partition(urns, MAX_TRACKS_BATCH))
                         .flatMap(fetchTracks);
    }

    private Observable<Urn> batchedAvailableTracks(List<Urn> urns) {
        return Observable.from(partition(urns, MAX_TRACKS_BATCH)).flatMap(fetchAvailableTrackUrns);
    }

    private Map<Urn, Track> toMapOfUrnAndTrack(List<QueryResult> cursorReadersBatches) {
        final Map<Urn, Track> tracks = new HashMap<>(cursorReadersBatches.size() * MAX_TRACKS_BATCH);
        for (QueryResult cursorReaders : cursorReadersBatches) {
            for (CursorReader cursorReader : cursorReaders) {
                final Track track = from(cursorReader);
                tracks.put(track.urn(), track);
            }
        }
        return tracks;
    }

    Observable<List<Urn>> availableTracks(final List<Urn> requestedTracks) {
        return batchedAvailableTracks(requestedTracks)
                .toList()
                .firstOrDefault(Collections.emptyList());
    }

    Observable<Optional<String>> loadTrackDescription(Urn urn) {
        return propeller.query(buildTrackDescriptionQuery(urn))
                        .map(new TrackDescriptionMapper())
                        .firstOrDefault(Optional.absent());
    }

    private Query buildTrackDescriptionQuery(Urn trackUrn) {
        return Query.from(Table.SoundView.name())
                    .select(TableColumns.SoundView.DESCRIPTION)
                    .whereEq(_ID, trackUrn.getNumericId())
                    .whereEq(_TYPE, TYPE_TRACK);
    }

    private Query buildTrackQuery(Urn trackUrn) {
        return Query.from(Tables.TrackView.TABLE)
                    .select("*")
                    .whereEq(Tables.TrackView.ID.name(), trackUrn.getNumericId());
    }

    private Query buildTracksQuery(List<Urn> trackUrns) {
        return Query.from(Tables.TrackView.TABLE)
                    .select("*")
                    .whereIn(Tables.TrackView.ID.name(), transform(trackUrns, Urns.TO_ID));
    }

    private Query buildAvailableTracksQuery(List<Urn> trackUrns) {
        return Query.from(Tables.Sounds.TABLE)
                    .select(Tables.Sounds._ID.as(BaseColumns._ID))
                    .whereEq(Tables.Sounds._TYPE, TYPE_TRACK)
                    .whereIn(Tables.Sounds._ID, transform(trackUrns, Urns.TO_ID));
    }


    private static Track from(CursorReader cursorReader) {
        final Track.Builder builder = Track.builder();
        builder.urn(Urn.forTrack(cursorReader.getLong(ID.name())));
        builder.title(cursorReader.getString(TITLE.name()));
        builder.snippetDuration(cursorReader.getLong(SNIPPET_DURATION.name()));
        builder.fullDuration(cursorReader.getLong(FULL_DURATION.name()));
        builder.playCount(cursorReader.getInt(PLAY_COUNT.name()));
        builder.commentsCount(cursorReader.getInt(COMMENTS_COUNT.name()));
        builder.commentable(cursorReader.getBoolean(IS_COMMENTABLE.name()));
        builder.likesCount(cursorReader.getInt(LIKES_COUNT.name()));
        builder.repostsCount(cursorReader.getInt(REPOSTS_COUNT.name()));
        builder.monetizable(cursorReader.getBoolean(MONETIZABLE.name()));
        builder.blocked(cursorReader.getBoolean(BLOCKED.name()));
        builder.snipped(cursorReader.getBoolean(SNIPPED.name()));
        builder.subHighTier(cursorReader.getBoolean(SUB_HIGH_TIER.name()));
        builder.subMidTier(cursorReader.getBoolean(SUB_MID_TIER.name()));
        builder.monetizationModel(cursorReader.getString(MONETIZATION_MODEL.name()));
        builder.userLike(cursorReader.getBoolean(IS_USER_LIKE.name()));
        builder.permalinkUrl(cursorReader.getString(PERMALINK_URL.name()));
        builder.userRepost(cursorReader.getBoolean(IS_USER_REPOST.name()));
        builder.isPrivate(SHARING_PRIVATE.equalsIgnoreCase(cursorReader.getString(SHARING.name())));
        builder.createdAt(cursorReader.getDateFromTimestamp(CREATED_AT.name()));
        builder.imageUrlTemplate(Optional.fromNullable(cursorReader.getString(ARTWORK_URL.name())));

        builder.genre(Optional.fromNullable(cursorReader.getString(Tables.TrackView.GENRE.name())));

        putOptionalFields(cursorReader, builder);
        putOptionalOfflineSyncDates(cursorReader, builder);

        builder.description(Optional.absent());
        return builder.build();
    }

    private static void putOptionalFields(CursorReader cursorReader, Track.Builder builder) {
        builder.policy(Optional.fromNullable(cursorReader.getString(POLICY.name())));
        builder.waveformUrl(Optional.fromNullable(cursorReader.getString(WAVEFORM_URL.name())));

        // synced tracks that might not have a user if they haven't been lazily updated yet
        builder.creatorName(Optional.fromNullable(cursorReader.getString(CREATOR_NAME.name())));
        final long creatorId = cursorReader.getLong(CREATOR_ID.name());
        builder.creatorUrn(creatorId == Consts.NOT_SET ? Optional.absent() : Optional.of(Urn.forUser(creatorId)));
    }

    private static void putOptionalOfflineSyncDates(CursorReader cursorReader, Track.Builder builder) {
        final Date defaultDate = new Date(0);
        final Date removedAt = getDateOr(cursorReader, OFFLINE_REMOVED_AT.name(), defaultDate);
        final Date downloadedAt = getDateOr(cursorReader, OFFLINE_DOWNLOADED_AT.name(), defaultDate);
        final Date requestedAt = getDateOr(cursorReader, OFFLINE_REQUESTED_AT.name(), defaultDate);
        final Date unavailableAt = getDateOr(cursorReader, OFFLINE_UNAVAILABLE_AT.name(), defaultDate);
        OfflineState offlineState = getOfflineState(true, requestedAt, removedAt, downloadedAt, unavailableAt);
        builder.offlineState(offlineState);
    }

    private static Date getDateOr(CursorReader cursorReader, String columnName, Date defaultDate) {
        if (cursorReader.isNotNull(columnName)) {
            return cursorReader.getDateFromTimestamp(columnName);
        }
        return defaultDate;
    }
}
