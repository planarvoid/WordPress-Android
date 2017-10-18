package com.soundcloud.android.tracks;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.TableColumns.ResourceTable._TYPE;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK;
import static com.soundcloud.android.storage.Tables.TrackView.ARTWORK_URL;
import static com.soundcloud.android.storage.Tables.TrackView.BLOCKED;
import static com.soundcloud.android.storage.Tables.TrackView.COMMENTS_COUNT;
import static com.soundcloud.android.storage.Tables.TrackView.CREATED_AT;
import static com.soundcloud.android.storage.Tables.TrackView.CREATOR_ID;
import static com.soundcloud.android.storage.Tables.TrackView.CREATOR_IS_PRO;
import static com.soundcloud.android.storage.Tables.TrackView.CREATOR_NAME;
import static com.soundcloud.android.storage.Tables.TrackView.FULL_DURATION;
import static com.soundcloud.android.storage.Tables.TrackView.ID;
import static com.soundcloud.android.storage.Tables.TrackView.IS_COMMENTABLE;
import static com.soundcloud.android.storage.Tables.TrackView.IS_USER_LIKE;
import static com.soundcloud.android.storage.Tables.TrackView.IS_USER_REPOST;
import static com.soundcloud.android.storage.Tables.TrackView.LIKES_COUNT;
import static com.soundcloud.android.storage.Tables.TrackView.MONETIZABLE;
import static com.soundcloud.android.storage.Tables.TrackView.MONETIZATION_MODEL;
import static com.soundcloud.android.storage.Tables.TrackView.PERMALINK_URL;
import static com.soundcloud.android.storage.Tables.TrackView.PLAY_COUNT;
import static com.soundcloud.android.storage.Tables.TrackView.POLICY;
import static com.soundcloud.android.storage.Tables.TrackView.POLICY_LAST_UPDATED_AT;
import static com.soundcloud.android.storage.Tables.TrackView.REPOSTS_COUNT;
import static com.soundcloud.android.storage.Tables.TrackView.SHARING;
import static com.soundcloud.android.storage.Tables.TrackView.SNIPPED;
import static com.soundcloud.android.storage.Tables.TrackView.SNIPPET_DURATION;
import static com.soundcloud.android.storage.Tables.TrackView.SUB_HIGH_TIER;
import static com.soundcloud.android.storage.Tables.TrackView.SUB_MID_TIER;
import static com.soundcloud.android.storage.Tables.TrackView.SYNCABLE;
import static com.soundcloud.android.storage.Tables.TrackView.TITLE;
import static com.soundcloud.android.storage.Tables.TrackView.WAVEFORM_URL;
import static com.soundcloud.java.collections.Lists.partition;
import static com.soundcloud.java.collections.MoreCollections.transform;
import static java.lang.String.format;

import com.soundcloud.android.Consts;
import com.soundcloud.android.commands.TrackUrnMapperV2;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRxV2;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackStorage {

    private static final int MAX_TRACKS_BATCH = 200;
    private static final String SHARING_PRIVATE = "private";
    private static final TrackUrnMapperV2 TRACK_URN_MAPPER = new TrackUrnMapperV2();

    private final PropellerRxV2 propeller;
    private final Function<List<Urn>, Observable<QueryResult>> fetchTracks = new Function<List<Urn>, Observable<QueryResult>>() {
        @Override
        public Observable<QueryResult> apply(List<Urn> urns) {
            return propeller.queryResult(buildTracksQuery(urns));
        }
    };

    private final Function<List<Urn>, Observable<Urn>> fetchAvailableTrackUrns = new Function<List<Urn>, Observable<Urn>>() {
        @Override
        public Observable<Urn> apply(List<Urn> urns) {
            return propeller.queryResult(buildAvailableTracksQuery(urns))
                            .flatMap(result -> Observable.fromIterable(result.toList(TRACK_URN_MAPPER)));
        }
    };

    @Inject
    public TrackStorage(PropellerRxV2 propeller) {
        this.propeller = propeller;
    }

    @SuppressWarnings("PMD.SimplifyStartsWith")
    public Maybe<Urn> urnForPermalink(String permalink) {
        if (permalink.startsWith("/")) {
            throw new IllegalArgumentException("Permalink must not start with a '/' and must not be a url.");
        }
        return propeller.queryResult(buildPermalinkQuery(permalink))
                        .filter(queryResult -> !queryResult.isEmpty())
                        .map(queryResult -> queryResult.first(cursorReader -> Urn.forTrack(cursorReader.getLong(Tables.TrackView.ID))))
                        .firstElement();
    }

    public Maybe<Track> loadTrack(Urn urn) {
        return propeller.queryResult(buildTrackQuery(urn))
                        .map(result -> result.toList(TrackStorage::trackFromCursorReader))
                        .map(tracks -> Iterables.transform(Iterables.filter(tracks, Optional::isPresent), Optional::get))
                        .flatMap(Observable::fromIterable)
                        .firstElement();
    }

    public Single<Map<Urn, Track>> loadTracks(List<Urn> urns) {
        return batchedTracks(urns).toList().map(this::toMapOfUrnAndTrack);
    }

    private Observable<QueryResult> batchedTracks(List<Urn> urns) {
        return Observable.fromIterable(partition(urns, MAX_TRACKS_BATCH)).flatMap(fetchTracks);
    }

    private Observable<Urn> batchedAvailableTracks(List<Urn> urns) {
        return Observable.fromIterable(partition(urns, MAX_TRACKS_BATCH)).flatMap(fetchAvailableTrackUrns);
    }

    private Map<Urn, Track> toMapOfUrnAndTrack(List<QueryResult> cursorReadersBatches) {
        final Map<Urn, Track> tracks = new HashMap<>(cursorReadersBatches.size() * MAX_TRACKS_BATCH);
        for (QueryResult cursorReaders : cursorReadersBatches) {
            for (CursorReader cursorReader : cursorReaders) {
                final Optional<Track> track = trackFromCursorReader(cursorReader);
                track.ifPresent(it -> tracks.put(it.urn(), it));
            }
        }
        return tracks;
    }

    Single<List<Urn>> availableTracks(final List<Urn> requestedTracks) {
        return batchedAvailableTracks(requestedTracks).toList();
    }

    Single<Optional<String>> loadTrackDescription(Urn urn) {
        return propeller.queryResult(buildTrackDescriptionQuery(urn))
                        .map(result -> result.toList(new TrackDescriptionMapper()))
                        .flatMap(Observable::fromIterable)
                        .first(Optional.absent());
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

    private Query buildPermalinkQuery(String permalink) {
        return Query.from(Tables.TrackView.TABLE)
                    .select(Tables.TrackView.ID)
                    .whereIn(Tables.TrackView.PERMALINK_URL, "https://soundcloud.com/" + permalink)
                    .limit(1);
    }

    public static Optional<Track> trackFromCursorReader(CursorReader cursorReader) {
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
        builder.isSyncable(cursorReader.getBoolean(SYNCABLE.name()));
        builder.snipped(cursorReader.getBoolean(SNIPPED.name()));
        builder.subHighTier(cursorReader.getBoolean(SUB_HIGH_TIER.name()));
        builder.subMidTier(cursorReader.getBoolean(SUB_MID_TIER.name()));
        builder.monetizationModel(Optional.fromNullable(cursorReader.getString(MONETIZATION_MODEL.name())).or(Strings.EMPTY));
        builder.userLike(cursorReader.getBoolean(IS_USER_LIKE.name()));
        builder.permalinkUrl(cursorReader.getString(PERMALINK_URL.name()));
        builder.userRepost(cursorReader.getBoolean(IS_USER_REPOST.name()));
        builder.isPrivate(SHARING_PRIVATE.equalsIgnoreCase(cursorReader.getString(SHARING.name())));
        builder.createdAt(cursorReader.getDateFromTimestamp(CREATED_AT.name()));
        builder.imageUrlTemplate(Optional.fromNullable(cursorReader.getString(ARTWORK_URL.name())));
        builder.creatorIsPro(cursorReader.getBoolean(CREATOR_IS_PRO.name()));

        builder.genre(Optional.fromNullable(cursorReader.getString(Tables.TrackView.GENRE.name())));
        builder.displayStatsEnabled(cursorReader.getBoolean(Tables.TrackView.DISPLAY_STATS_ENABLED.name()));

        builder.policyLastUpdatedAt(getDateOr(cursorReader, POLICY_LAST_UPDATED_AT.name(), new Date(0)));
        builder.waveformUrl(Optional.fromNullable(cursorReader.getString(WAVEFORM_URL.name())).or(Strings.EMPTY));

        // synced tracks that might not have a user if they haven't been lazily updated yet
        builder.creatorName(Optional.fromNullable(cursorReader.getString(CREATOR_NAME.name())).or(Strings.EMPTY));
        final long creatorId = cursorReader.getLong(CREATOR_ID.name());
        builder.creatorUrn(creatorId == Consts.NOT_SET ? Urn.NOT_SET : Urn.forUser(creatorId));

        builder.description(Optional.absent());
        String policy = cursorReader.getString(POLICY.name());
        if (policy == null) {
            ErrorUtils.handleSilentException(new IllegalStateException(format("Track found without policy: %s", builder.policy("<missing>").build())));
            return Optional.absent();
        } else {
            return Optional.of(builder.policy(policy).build());
        }
    }

    private static Date getDateOr(CursorReader cursorReader, String columnName, Date defaultDate) {
        if (cursorReader.isNotNull(columnName)) {
            return cursorReader.getDateFromTimestamp(columnName);
        }
        return defaultDate;
    }
}
