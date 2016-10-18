package com.soundcloud.android.commands;

import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns.Sounds;
import com.soundcloud.android.storage.TableColumns.TrackPolicies;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

                final List<ContentValues> trackValues = new ArrayList<>(Iterables.size(input));
                final List<ContentValues> policyValues = new ArrayList<>(Iterables.size(input));
                for (TrackRecord track : input) {
                    trackValues.add(buildTrackContentValues(track));
                    policyValues.add(buildPolicyContentValues(track));
                }
                step(propeller.bulkInsert_experimental(Table.Sounds, getTrackColumnTypes(), trackValues));
                step(propeller.bulkInsert_experimental(Table.TrackPolicies, getPolicyColumnTypes(), policyValues));
            }
        });
    }

    private Map<String, Class> getTrackColumnTypes() {
        final HashMap<String, Class> columns = new HashMap<>(19);
        columns.put(Sounds._ID, Long.class);
        columns.put(Sounds._TYPE, Integer.class);
        columns.put(Sounds.TITLE, String.class);
        columns.put(Sounds.FULL_DURATION, Long.class);
        columns.put(Sounds.SNIPPET_DURATION, Long.class);
        columns.put(Sounds.WAVEFORM_URL, String.class);
        columns.put(Sounds.STREAM_URL, String.class);
        columns.put(Sounds.PERMALINK_URL, String.class);
        columns.put(Sounds.ARTWORK_URL, String.class);
        columns.put(Sounds.CREATED_AT, Long.class);
        columns.put(Sounds.GENRE, String.class);
        columns.put(Sounds.SHARING, String.class);
        columns.put(Sounds.COMMENTABLE, Boolean.class);
        columns.put(Sounds.PLAYBACK_COUNT, Integer.class);
        columns.put(Sounds.COMMENT_COUNT, Integer.class);
        columns.put(Sounds.LIKES_COUNT, Integer.class);
        columns.put(Sounds.REPOSTS_COUNT, Integer.class);
        columns.put(Sounds.USER_ID, Long.class);
        columns.put(Sounds.DESCRIPTION, String.class);
        return columns;

    }

    public static ContentValues buildTrackContentValues(TrackRecord trackRecord) {
        return ContentValuesBuilder.values()
                .put(Sounds._ID, trackRecord.getUrn().getNumericId())
                .put(Sounds._TYPE, Sounds.TYPE_TRACK)
                .put(Sounds.TITLE, trackRecord.getTitle())
                .put(Sounds.FULL_DURATION, trackRecord.getFullDuration())
                .put(Sounds.SNIPPET_DURATION, trackRecord.getSnippetDuration())
                .put(Sounds.WAVEFORM_URL, trackRecord.getWaveformUrl())
                .put(Sounds.STREAM_URL, trackRecord.getStreamUrl())
                .put(Sounds.PERMALINK_URL, trackRecord.getPermalinkUrl())
                .put(Sounds.ARTWORK_URL, trackRecord.getImageUrlTemplate().orNull())
                .put(Sounds.CREATED_AT, trackRecord.getCreatedAt().getTime())
                .put(Sounds.GENRE, trackRecord.getGenre())
                .put(Sounds.SHARING, trackRecord.getSharing().value())
                .put(Sounds.COMMENTABLE, trackRecord.isCommentable())
                .put(Sounds.PLAYBACK_COUNT, trackRecord.getPlaybackCount())
                .put(Sounds.COMMENT_COUNT, trackRecord.getCommentsCount())
                .put(Sounds.LIKES_COUNT, trackRecord.getLikesCount())
                .put(Sounds.REPOSTS_COUNT, trackRecord.getRepostsCount())
                .put(Sounds.USER_ID, trackRecord.getUser().getUrn().getNumericId())
                .put(Sounds.DESCRIPTION, trackRecord.getDescription().orNull())
                .get();
    }

    public static ContentValues buildPolicyContentValues(TrackRecord trackRecord) {
        final ContentValuesBuilder valuesBuilder = ContentValuesBuilder.values();
        valuesBuilder
                .put(TrackPolicies.TRACK_ID, trackRecord.getUrn().getNumericId())
                .put(TrackPolicies.MONETIZABLE, trackRecord.isMonetizable())
                .put(TrackPolicies.BLOCKED, trackRecord.isBlocked())
                .put(TrackPolicies.SNIPPED, trackRecord.isSnipped())
                .put(TrackPolicies.POLICY, trackRecord.getPolicy())
                .put(TrackPolicies.SYNCABLE, trackRecord.isSyncable())
                .put(TrackPolicies.LAST_UPDATED, System.currentTimeMillis())
                .put(TrackPolicies.MONETIZATION_MODEL, trackRecord.getMonetizationModel().orNull())
                .put(TrackPolicies.SUB_MID_TIER, trackRecord.isSubMidTier().or(false))
                .put(TrackPolicies.SUB_HIGH_TIER, trackRecord.isSubHighTier().or(false));
        return valuesBuilder.get();
    }

    private Map<String, Class> getPolicyColumnTypes() {
        final HashMap<String, Class> columns = new HashMap<>();
        columns.put(TrackPolicies.TRACK_ID, Long.class);
        columns.put(TrackPolicies.MONETIZABLE, Boolean.class);
        columns.put(TrackPolicies.BLOCKED, Boolean.class);
        columns.put(TrackPolicies.SNIPPED, Boolean.class);
        columns.put(TrackPolicies.POLICY, String.class);
        columns.put(TrackPolicies.SYNCABLE, Boolean.class);
        columns.put(TrackPolicies.LAST_UPDATED, Long.class);
        columns.put(TrackPolicies.MONETIZATION_MODEL, String.class);
        columns.put(TrackPolicies.SUB_MID_TIER, Boolean.class);
        columns.put(TrackPolicies.SUB_HIGH_TIER, Boolean.class);
        return columns;
    }
}
