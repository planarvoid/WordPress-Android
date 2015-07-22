package com.soundcloud.android.tracks;

import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.query.Filter;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import javax.inject.Inject;

class TrackStorage {

    private final PropellerRx propeller;

    @Inject
    TrackStorage(PropellerRx propeller) {
        this.propeller = propeller;
    }

    Observable<PropertySet> loadTrack(Urn urn) {
        return propeller.query(buildTrackQuery(urn))
                .map(new TrackItemMapper())
                .firstOrDefault(PropertySet.create());
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
        return Query.from(Table.SoundView.name())
                .select(
                        TableColumns.SoundView._ID,
                        TableColumns.SoundView.TITLE,
                        TableColumns.SoundView.USERNAME,
                        TableColumns.SoundView.USER_ID,
                        TableColumns.SoundView.DURATION,
                        TableColumns.SoundView.PLAYBACK_COUNT,
                        TableColumns.SoundView.COMMENT_COUNT,
                        TableColumns.SoundView.LIKES_COUNT,
                        TableColumns.SoundView.REPOSTS_COUNT,
                        TableColumns.SoundView.WAVEFORM_URL,
                        TableColumns.SoundView.STREAM_URL,
                        TableColumns.SoundView.POLICIES_MONETIZABLE,
                        TableColumns.SoundView.POLICIES_POLICY,
                        TableColumns.SoundView.PERMALINK_URL,
                        TableColumns.SoundView.SHARING,
                        TableColumns.SoundView.CREATED_AT,
                        TableColumns.SoundView.OFFLINE_DOWNLOADED_AT,
                        TableColumns.SoundView.OFFLINE_REMOVED_AT,
                        exists(likeQuery(trackUrn)).as(TableColumns.SoundView.USER_LIKE),
                        exists(repostQuery(trackUrn)).as(TableColumns.SoundView.USER_REPOST)
                )
                .whereEq(TableColumns.SoundView._ID, trackUrn.getNumericId())
                .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_TRACK);
    }

    private Query likeQuery(Urn trackUrn) {
        final Where joinConditions = Filter.filter()
                .whereEq(Table.Sounds.field(TableColumns.Sounds._ID), Table.Likes.field(TableColumns.Likes._ID))
                .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), Table.Likes.field(TableColumns.Likes._TYPE));

        return Query.from(Table.Likes.name())
                .innerJoin(Table.Sounds.name(), joinConditions)
                .whereEq(Table.Sounds.field(TableColumns.Sounds._ID), trackUrn.getNumericId())
                .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_TRACK)
                .whereNull(TableColumns.Likes.REMOVED_AT);
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

}
