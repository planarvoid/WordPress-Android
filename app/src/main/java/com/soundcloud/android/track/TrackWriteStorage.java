package com.soundcloud.android.track;

import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.UserSummary;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.BulkResult;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import rx.Observable;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.List;

public class TrackWriteStorage {

    private final DatabaseScheduler scheduler;

    @Inject
    public TrackWriteStorage(DatabaseScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Observable<ChangeResult> storeTrackAsync(final TrackSummary track) {
        return scheduler.scheduleTransaction(new PropellerDatabase.Transaction<ChangeResult>() {
            @Override
            public ChangeResult execute(PropellerDatabase propeller) {
                step(propeller.upsert(Table.USERS.name, TableColumns.Users._ID, buildUserContentValues(track.getUser())));
                return step(propeller.upsert(Table.SOUNDS.name, TableColumns.Sounds._ID, buildTrackContentValues(track)));
            }

        });
    }

    public Observable<BulkResult<ChangeResult>> storeTracksAsync(final List<TrackSummary> tracks) {
        return scheduler.scheduleTransaction(new PropellerDatabase.Transaction<BulkResult<ChangeResult>>() {
            @Override
            public BulkResult<ChangeResult> execute(PropellerDatabase propeller) {
                final BulkResult<ChangeResult> result = new BulkResult<ChangeResult>(tracks.size() * 2);
                for (TrackSummary track : tracks) {
                    result.add(step(propeller.upsert(Table.USERS.name, TableColumns.Users._ID, buildUserContentValues(track.getUser()))));
                    result.add(step(propeller.upsert(Table.SOUNDS.name, TableColumns.Sounds._ID, buildTrackContentValues(track))));
                }
                return result;
            }
        });
    }

    private ContentValues buildTrackContentValues(TrackSummary track) {
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

    private ContentValues buildUserContentValues(UserSummary user) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Users._ID, user.getId())
                .put(TableColumns.Users.USERNAME, user.getUsername())
                .get();
    }

}
