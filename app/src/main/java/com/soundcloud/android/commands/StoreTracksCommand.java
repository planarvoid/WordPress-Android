package com.soundcloud.android.commands;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;

public class StoreTracksCommand extends StoreCommand<Iterable<ApiTrack>> {

    @Inject
    public StoreTracksCommand(PropellerDatabase database) {
        super(database);
    }

    public static ContentValues buildTrackContentValues(ApiTrack track) {
        if (track.getTitle() == null) {
            ErrorUtils.handleSilentException(new IllegalStateException("Inserting a track with a NULL title: " + track.getUrn()));
        }
        return ContentValuesBuilder.values()
                .put(TableColumns.Sounds._ID, track.getId())
                .put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .put(TableColumns.Sounds.TITLE, track.getTitle())
                .put(TableColumns.Sounds.DURATION, track.getDuration())
                .put(TableColumns.Sounds.WAVEFORM_URL, track.getWaveformUrl())
                .put(TableColumns.Sounds.STREAM_URL, track.getStreamUrl())
                .put(TableColumns.Sounds.PERMALINK_URL, track.getPermalinkUrl())
                .put(TableColumns.Sounds.CREATED_AT, track.getCreatedAt().getTime())
                .put(TableColumns.Sounds.GENRE, track.getGenre())
                .put(TableColumns.Sounds.SHARING, track.getSharing().value())
                .put(TableColumns.Sounds.COMMENTABLE, track.isCommentable())
                .put(TableColumns.Sounds.PLAYBACK_COUNT, track.getStats().getPlaybackCount())
                .put(TableColumns.Sounds.COMMENT_COUNT, track.getStats().getCommentsCount())
                .put(TableColumns.Sounds.LIKES_COUNT, track.getStats().getLikesCount())
                .put(TableColumns.Sounds.REPOSTS_COUNT, track.getStats().getRepostsCount())
                .put(TableColumns.Sounds.USER_ID, track.getUser().getId())
                .get();
    }

    public static ContentValues buildPolicyContentValues(ApiTrack track) {
        return ContentValuesBuilder.values()
                .put(TableColumns.TrackPolicies.TRACK_ID, track.getId())
                .put(TableColumns.TrackPolicies.MONETIZABLE, track.isMonetizable())
                .put(TableColumns.TrackPolicies.POLICY, track.getPolicy())
                .put(TableColumns.TrackPolicies.SYNCABLE, track.isSyncable())
                .put(TableColumns.TrackPolicies.LAST_UPDATED, System.currentTimeMillis())
                .get();
    }

    @Override
    protected WriteResult store() {
        return database.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (ApiTrack track : input) {
                    step(propeller.upsert(Table.Users, StoreUsersCommand.buildUserContentValues(track.getUser())));
                    step(propeller.upsert(Table.Sounds, buildTrackContentValues(track)));
                    step(propeller.upsert(Table.TrackPolicies, buildPolicyContentValues(track)));
                }
            }
        });
    }
}
