package com.soundcloud.android.commands;

import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.schema.BulkInsertValues;
import com.soundcloud.propeller.schema.Column;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

public class StoreTracksCommand extends DefaultWriteStorageCommand<Iterable<? extends TrackRecord>, WriteResult> {

    private final StoreUsersCommand storeUsersCommand;

    @Inject
    public StoreTracksCommand(PropellerDatabase database, StoreUsersCommand storeUsersCommand) {
        super(database);
        this.storeUsersCommand = storeUsersCommand;
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final Iterable<? extends TrackRecord> input) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(storeUsersCommand.write(propeller, Iterables.transform(input, TrackRecord.TO_USER_RECORD)));
                step(propeller.bulkInsert(Tables.Sounds.TABLE, getTrackBulkValues(input)));
                step(propeller.bulkInsert(Tables.TrackPolicies.TABLE, getPolicyBulkValues(input)));
            }
        });
    }

    private static BulkInsertValues getTrackBulkValues(Iterable<? extends TrackRecord> trackRecords) {
        final BulkInsertValues.Builder trackValues = new BulkInsertValues.Builder(getTrackColumns());
        for (TrackRecord track : trackRecords) {
            trackValues.addRow(buildTrackRow(track));
        }
        return trackValues.build();
    }

    private static BulkInsertValues getPolicyBulkValues(Iterable<? extends TrackRecord> trackRecords) {
        final BulkInsertValues.Builder policyValues = new BulkInsertValues.Builder(getPolicyColumns());
        for (TrackRecord track : trackRecords) {
            policyValues.addRow(buildPolicyRow(track));
        }
        return policyValues.build();
    }

    private static List<Column> getTrackColumns() {
        return Arrays.asList(
                Tables.Sounds._ID,
                Tables.Sounds._TYPE,
                Tables.Sounds.TITLE,
                Tables.Sounds.FULL_DURATION,
                Tables.Sounds.SNIPPET_DURATION,
                Tables.Sounds.WAVEFORM_URL,
                Tables.Sounds.STREAM_URL,
                Tables.Sounds.PERMALINK_URL,
                Tables.Sounds.ARTWORK_URL,
                Tables.Sounds.CREATED_AT,
                Tables.Sounds.GENRE,
                Tables.Sounds.SHARING,
                Tables.Sounds.COMMENTABLE,
                Tables.Sounds.PLAYBACK_COUNT,
                Tables.Sounds.COMMENT_COUNT,
                Tables.Sounds.LIKES_COUNT,
                Tables.Sounds.REPOSTS_COUNT,
                Tables.Sounds.USER_ID,
                Tables.Sounds.DESCRIPTION
        );
    }

    private static List<Column> getPolicyColumns() {
        return Arrays.asList(
                Tables.TrackPolicies.TRACK_ID,
                Tables.TrackPolicies.MONETIZABLE,
                Tables.TrackPolicies.BLOCKED,
                Tables.TrackPolicies.SNIPPED,
                Tables.TrackPolicies.POLICY,
                Tables.TrackPolicies.SYNCABLE,
                Tables.TrackPolicies.LAST_UPDATED,
                Tables.TrackPolicies.MONETIZATION_MODEL,
                Tables.TrackPolicies.SUB_MID_TIER,
                Tables.TrackPolicies.SUB_HIGH_TIER
        );
    }

    public static List<Object> buildTrackRow(TrackRecord trackRecord) {
        return Arrays.asList(
                trackRecord.getUrn().getNumericId(),
                Tables.Sounds.TYPE_TRACK,
                trackRecord.getTitle(),
                trackRecord.getFullDuration(),
                trackRecord.getSnippetDuration(),
                trackRecord.getWaveformUrl(),
                trackRecord.getStreamUrl(),
                trackRecord.getPermalinkUrl(),
                trackRecord.getImageUrlTemplate().orNull(),
                trackRecord.getCreatedAt().getTime(),
                trackRecord.getGenre(),
                trackRecord.getSharing().value(),
                trackRecord.isCommentable(),
                trackRecord.getPlaybackCount(),
                trackRecord.getCommentsCount(),
                trackRecord.getLikesCount(),
                trackRecord.getRepostsCount(),
                trackRecord.getUser().getUrn().getNumericId(),
                trackRecord.getDescription().orNull()
        );
    }

    public static List<Object> buildPolicyRow(TrackRecord trackRecord) {
        return Arrays.asList(
                trackRecord.getUrn().getNumericId(),
                trackRecord.isMonetizable(),
                trackRecord.isBlocked(),
                trackRecord.isSnipped(),
                trackRecord.getPolicy(),
                trackRecord.isSyncable(),
                System.currentTimeMillis(),
                trackRecord.getMonetizationModel().orNull(),
                trackRecord.isSubMidTier().or(false),
                trackRecord.isSubHighTier().or(false)
        );
    }
}
