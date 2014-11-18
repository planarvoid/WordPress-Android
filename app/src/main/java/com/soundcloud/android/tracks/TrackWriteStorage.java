package com.soundcloud.android.tracks;

import static com.soundcloud.android.users.UserWriteStorage.buildUserContentValues;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.PolicyInfo;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import rx.Observable;
import rx.Scheduler;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TrackWriteStorage {

    private final DatabaseScheduler scheduler;
    private final PropellerDatabase database;

    @Inject
    public TrackWriteStorage(PropellerDatabase database, Scheduler scheduler) {
        this.database = database;
        this.scheduler = new DatabaseScheduler(database, scheduler);
    }

    public Observable<TxnResult> storeTrackAsync(final ApiTrack track) {
        return scheduler.scheduleTransaction(storeTracksTransaction(Arrays.asList(track)));
    }

    public Observable<TxnResult> storeTracksAsync(final Collection<ApiTrack> tracks) {
        return scheduler.scheduleTransaction(storeTracksTransaction(tracks));
    }

    public TxnResult storeTracks(List<ApiTrack> tracks) {
        return database.runTransaction(storeTracksTransaction(tracks));
    }

    private PropellerDatabase.Transaction storeTracksTransaction(final Collection<ApiTrack> tracks) {
        return new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (ApiTrack track : tracks) {
                    step(propeller.upsert(Table.Users, buildUserContentValues(track.getUser())));
                    step(propeller.upsert(Table.Sounds, buildTrackContentValues(track)));
                }
            }
        };
    }

    public Observable<TxnResult> storePoliciesAsync(final Collection<PolicyInfo> policies){
        return scheduler.scheduleTransaction(storePoliciesTransaction(policies));
    }

    private PropellerDatabase.Transaction storePoliciesTransaction(final Collection<PolicyInfo> policies){
        return new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (PolicyInfo policyInfo : policies) {
                    step(propeller.upsert(Table.Sounds, buildPolicyContentValues(policyInfo)));
                }
            }
        };
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
                .put(TableColumns.Sounds.MONETIZABLE, track.isMonetizable())
                .put(TableColumns.Sounds.PLAYBACK_COUNT, track.getStats().getPlaybackCount())
                .put(TableColumns.Sounds.COMMENT_COUNT, track.getStats().getCommentsCount())
                .put(TableColumns.Sounds.LIKES_COUNT, track.getStats().getLikesCount())
                .put(TableColumns.Sounds.REPOSTS_COUNT, track.getStats().getRepostsCount())
                .put(TableColumns.Sounds.USER_ID, track.getUser().getId())
                .get();
    }

    private ContentValues buildPolicyContentValues(PolicyInfo policyEntry) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Sounds._ID, policyEntry.getTrackUrn().getNumericId())
                .put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .put(TableColumns.Sounds.POLICY, policyEntry.getPolicy())
                .put(TableColumns.Sounds.MONETIZABLE, policyEntry.isMonetizable())
                .get();
    }

}
