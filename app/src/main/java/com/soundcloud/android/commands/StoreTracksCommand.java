package com.soundcloud.android.commands;

import static com.soundcloud.android.commands.StoreUsersCommand.buildUserContentValues;

import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns.Sounds;
import com.soundcloud.android.storage.TableColumns.TrackPolicies;
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
                    step(propeller.upsert(Table.Users, buildUserContentValues(trackRecord.getUser())));
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
        final ContentValuesBuilder valuesBuilder = ContentValuesBuilder.values();
        valuesBuilder
                .put(Sounds._ID, trackRecord.getUrn().getNumericId())
                .put(Sounds._TYPE, Sounds.TYPE_TRACK)
                .put(Sounds.TITLE, trackRecord.getTitle())
                .put(Sounds.DURATION, trackRecord.getDuration())
                .put(Sounds.WAVEFORM_URL, trackRecord.getWaveformUrl())
                .put(Sounds.STREAM_URL, trackRecord.getStreamUrl())
                .put(Sounds.PERMALINK_URL, trackRecord.getPermalinkUrl())
                .put(Sounds.CREATED_AT, trackRecord.getCreatedAt().getTime())
                .put(Sounds.GENRE, trackRecord.getGenre())
                .put(Sounds.SHARING, trackRecord.getSharing().value())
                .put(Sounds.COMMENTABLE, trackRecord.isCommentable())
                .put(Sounds.PLAYBACK_COUNT, trackRecord.getPlaybackCount())
                .put(Sounds.COMMENT_COUNT, trackRecord.getCommentsCount())
                .put(Sounds.LIKES_COUNT, trackRecord.getLikesCount())
                .put(Sounds.REPOSTS_COUNT, trackRecord.getRepostsCount())
                .put(Sounds.USER_ID, trackRecord.getUser().getUrn().getNumericId());
        if (trackRecord.getDescription().isPresent()) {
            valuesBuilder.put(Sounds.DESCRIPTION, trackRecord.getDescription().get());
        }
        return valuesBuilder.get();
    }

    public static ContentValues buildPolicyContentValues(TrackRecord trackRecord) {
        final ContentValuesBuilder valuesBuilder = ContentValuesBuilder.values()
                .put(TrackPolicies.TRACK_ID, trackRecord.getUrn().getNumericId())
                .put(TrackPolicies.MONETIZABLE, trackRecord.isMonetizable())
                .put(TrackPolicies.POLICY, trackRecord.getPolicy())
                .put(TrackPolicies.SYNCABLE, trackRecord.isSyncable())
                .put(TrackPolicies.LAST_UPDATED, System.currentTimeMillis());

        if (trackRecord.getMonetizationModel().isPresent()) {
            valuesBuilder.put(TrackPolicies.MONETIZATION_MODEL, trackRecord.getMonetizationModel().get());
        }
        if (trackRecord.isSubMidTier().isPresent()) {
            valuesBuilder.put(TrackPolicies.SUB_MID_TIER, trackRecord.isSubMidTier().get());
        }
        if (trackRecord.isSubHighTier().isPresent()) {
            valuesBuilder.put(TrackPolicies.SUB_HIGH_TIER, trackRecord.isSubHighTier().get());
        }
        return valuesBuilder.get();
    }
}
