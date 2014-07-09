package com.soundcloud.android.track;

import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import rx.Observable;

import android.content.ContentValues;

import javax.inject.Inject;

public class TrackWriteStorage {

    private final DatabaseScheduler scheduler;

    @Inject
    public TrackWriteStorage(DatabaseScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Observable<InsertResult> storeTrack(final TrackSummary track) {
        return scheduler.scheduleTransaction(new PropellerDatabase.Transaction<InsertResult>() {
            @Override
            public InsertResult execute(PropellerDatabase propeller) {
                if (!propeller.insert(Table.USERS.name, buildUserContentValues(track)).success()) {
                    fail();
                }
                return propeller.insert(Table.SOUNDS.name, buildTrackContentValues(track));
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

    private ContentValues buildUserContentValues(TrackSummary track) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Users._ID, track.getUser().getId())
                .put(TableColumns.Users.USERNAME, track.getUser().getUsername())
                .get();
    }

}
