package com.soundcloud.android.commands;

import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;

public class StoreTracksCommand extends DefaultWriteStorageCommand<Iterable<? extends TrackRecord>, WriteResult> {

    @Inject
    public StoreTracksCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final Iterable<? extends TrackRecord> input) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (TrackRecord trackRecord : input) {
                    step(propeller.upsert(Table.Users, StoreUsersCommand.buildUserContentValues(trackRecord.getUser())));
                    step(propeller.upsert(Table.Sounds, buildTrackContentValues(trackRecord)));
                    step(propeller.upsert(Table.TrackPolicies, buildPolicyContentValues(trackRecord)));
                }
            }
        });
    }

    public static ContentValues buildTrackContentValues(TrackRecord trackRecord) {
        if (trackRecord.getTitle() == null) {
            ErrorUtils.handleSilentException(new IllegalStateException("Inserting a track with a NULL title: " + trackRecord.getUrn()));
        }
        return ContentValuesBuilder.values()
                .put(TableColumns.Sounds._ID, trackRecord.getUrn().getNumericId())
                .put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .put(TableColumns.Sounds.TITLE, trackRecord.getTitle())
                .put(TableColumns.Sounds.DURATION, trackRecord.getDuration())
                .put(TableColumns.Sounds.WAVEFORM_URL, trackRecord.getWaveformUrl())
                .put(TableColumns.Sounds.STREAM_URL, trackRecord.getStreamUrl())
                .put(TableColumns.Sounds.PERMALINK_URL, trackRecord.getPermalinkUrl())
                .put(TableColumns.Sounds.CREATED_AT, trackRecord.getCreatedAt().getTime())
                .put(TableColumns.Sounds.GENRE, trackRecord.getGenre())
                .put(TableColumns.Sounds.SHARING, trackRecord.getSharing().value())
                .put(TableColumns.Sounds.COMMENTABLE, trackRecord.isCommentable())
                .put(TableColumns.Sounds.PLAYBACK_COUNT, trackRecord.getPlaybackCount())
                .put(TableColumns.Sounds.COMMENT_COUNT, trackRecord.getCommentsCount())
                .put(TableColumns.Sounds.LIKES_COUNT, trackRecord.getLikesCount())
                .put(TableColumns.Sounds.REPOSTS_COUNT, trackRecord.getRepostsCount())
                .put(TableColumns.Sounds.USER_ID, trackRecord.getUser().getUrn().getNumericId())
                .get();
    }

    public static ContentValues buildPolicyContentValues(TrackRecord trackRecord) {
        final ContentValuesBuilder valuesBuilder = ContentValuesBuilder.values()
                .put(TableColumns.TrackPolicies.TRACK_ID, trackRecord.getUrn().getNumericId())
                .put(TableColumns.TrackPolicies.MONETIZABLE, trackRecord.isMonetizable())
                .put(TableColumns.TrackPolicies.POLICY, trackRecord.getPolicy())
                .put(TableColumns.TrackPolicies.SYNCABLE, trackRecord.isSyncable())
                .put(TableColumns.TrackPolicies.LAST_UPDATED, System.currentTimeMillis());

        if (trackRecord.getMonetizationModel().isPresent()) {
            valuesBuilder.put(TableColumns.TrackPolicies.MONETIZATION_MODEL, trackRecord.getMonetizationModel().get());
        }
        if (trackRecord.isSubMidTier().isPresent()) {
            valuesBuilder.put(TableColumns.TrackPolicies.SUB_MID_TIER, trackRecord.isSubMidTier().get());
        }
        if (trackRecord.isSubHighTier().isPresent()) {
            valuesBuilder.put(TableColumns.TrackPolicies.SUB_HIGH_TIER, trackRecord.isSubHighTier().get());
        }
        return valuesBuilder.get();
    }
}
