package com.soundcloud.android.tracks;

import static com.soundcloud.android.tracks.TrackItemMapper.BASE_TRACK_FIELDS;
import static com.soundcloud.java.collections.MoreCollections.transform;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Filter;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TrackStorage {

    private final PropellerRx propeller;
    private final TrackItemMapper trackMapper = new TrackItemMapper();
    private Func1<QueryResult, Map<Urn, PropertySet>> toMapOfUrnAndPropertySet = new Func1<QueryResult, Map<Urn, PropertySet>>() {
        @Override
        public Map<Urn, PropertySet> call(QueryResult cursorReaders) {
            return toMapOfUrnAndPropertySet(cursorReaders);
        }
    };

    @Inject
    TrackStorage(PropellerRx propeller) {
        this.propeller = propeller;
    }

    Observable<PropertySet> loadTrack(Urn urn) {
        return propeller.query(buildTrackQuery(urn))
                        .map(trackMapper)
                        .firstOrDefault(PropertySet.create());
    }

    Observable<Map<Urn, PropertySet>> loadTracks(List<Urn> urns) {
        return propeller.queryResult(buildTracksQuery(urns))
                        .map(toMapOfUrnAndPropertySet)
                        .firstOrDefault(Collections.<Urn, PropertySet>emptyMap());
    }

    private Map<Urn, PropertySet> toMapOfUrnAndPropertySet(QueryResult cursorReaders) {
        final Map<Urn, PropertySet> tracks = new HashMap<>(cursorReaders.getResultCount());
        for (CursorReader cursorReader : cursorReaders) {
            final PropertySet track = trackMapper.map(cursorReader);
            final Urn urn = track.get(TrackProperty.URN);

            tracks.put(urn, track);
        }
        return tracks;
    }

    public Observable<List<Urn>> availableTracks(final List<Urn> requestedTracks) {
        return propeller
                .query(buildTracksQuery(requestedTracks))
                .map(new TrackUrnMapper())
                .toList();
    }

    Observable<PropertySet> loadTrackDescription(Urn urn) {
        return propeller.query(buildTrackDescriptionQuery(urn))
                        .map(new TrackDescriptionMapper())
                        .firstOrDefault(PropertySet.create());
    }

    private Query buildTrackDescriptionQuery(Urn trackUrn) {
        return Query.from(Table.SoundView.name())
                    .select(TableColumns.SoundView.DESCRIPTION)
                    .whereEq(TableColumns.SoundView._ID, trackUrn.getNumericId())
                    .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_TRACK);
    }

    private Query buildTrackQuery(Urn trackUrn) {
        List<Object> fields = new ArrayList<>(BASE_TRACK_FIELDS.size() + 2);
        fields.addAll(BASE_TRACK_FIELDS);
        fields.add(exists(likeQuery(trackUrn)).as(TableColumns.SoundView.USER_LIKE));
        fields.add(exists(repostQuery(trackUrn)).as(TableColumns.SoundView.USER_REPOST));

        return Query.from(Table.SoundView.name())
                    .select(fields.toArray())
                    .whereEq(TableColumns.SoundView._ID, trackUrn.getNumericId())
                    .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_TRACK);
    }

    private Query buildTracksQuery(List<Urn> trackUrn) {
        List<Object> fields = new ArrayList<>(BASE_TRACK_FIELDS.size() + 2);
        fields.addAll(BASE_TRACK_FIELDS);
        fields.add(exists(likesQuery(trackUrn)).as(TableColumns.SoundView.USER_LIKE));
        fields.add(exists(repostsQuery(trackUrn)).as(TableColumns.SoundView.USER_REPOST));

        return Query.from(Table.SoundView.name())
                    .select(fields.toArray())
                    .whereIn(TableColumns.SoundView._ID, transform(trackUrn, Urns.TO_ID))
                    .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_TRACK);
    }

    private Query likesQuery(List<Urn> trackUrn) {
        final Where joinConditions = Filter.filter()
                                           .whereEq(Table.Sounds.field(TableColumns.Sounds._ID),
                                                    Table.Likes.field(TableColumns.Likes._ID))
                                           .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE),
                                                    Table.Likes.field(TableColumns.Likes._TYPE));

        return Query.from(Table.Likes.name())
                    .innerJoin(Table.Sounds.name(), joinConditions)
                    .whereIn(Table.Sounds.field(TableColumns.Sounds._ID), transform(trackUrn, Urns.TO_ID))
                    .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_TRACK)
                    .whereNull(Table.Likes.field(TableColumns.Likes.REMOVED_AT));
    }


    private Query likeQuery(Urn trackUrn) {
        final Where joinConditions = Filter.filter()
                                           .whereEq(Table.Sounds.field(TableColumns.Sounds._ID),
                                                    Table.Likes.field(TableColumns.Likes._ID))
                                           .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE),
                                                    Table.Likes.field(TableColumns.Likes._TYPE));

        return Query.from(Table.Likes.name())
                    .innerJoin(Table.Sounds.name(), joinConditions)
                    .whereEq(Table.Sounds.field(TableColumns.Sounds._ID), trackUrn.getNumericId())
                    .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_TRACK)
                    .whereNull(Table.Likes.field(TableColumns.Likes.REMOVED_AT));
    }

    private Query repostQuery(Urn trackUrn) {
        final Where joinConditions = Filter.filter()
                                           .whereEq(TableColumns.Sounds._ID, TableColumns.Posts.TARGET_ID)
                                           .whereEq(TableColumns.Sounds._TYPE, TableColumns.Posts.TARGET_TYPE);

        return Query.from(Table.Posts.name())
                    .innerJoin(Table.Sounds.name(), joinConditions)
                    .whereEq(TableColumns.Sounds._ID, trackUrn.getNumericId())
                    .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_TRACK)
                    .whereEq(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_REPOST);
    }

    private Query repostsQuery(List<Urn> trackUrn) {
        final Where joinConditions = Filter.filter()
                                           .whereEq(TableColumns.Sounds._ID, TableColumns.Posts.TARGET_ID)
                                           .whereEq(TableColumns.Sounds._TYPE, TableColumns.Posts.TARGET_TYPE);

        return Query.from(Table.Posts.name())
                    .innerJoin(Table.Sounds.name(), joinConditions)
                    .whereIn(Table.Sounds.field(TableColumns.Sounds._ID), transform(trackUrn, Urns.TO_ID))
                    .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_TRACK)
                    .whereEq(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_REPOST);
    }

}
