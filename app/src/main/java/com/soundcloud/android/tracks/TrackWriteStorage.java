package com.soundcloud.android.tracks;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import rx.Observable;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;

public class TrackWriteStorage {

    private final DatabaseScheduler scheduler;

    @Inject
    public TrackWriteStorage(DatabaseScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Observable<TxnResult> storeTrackAsync(final ApiTrack track) {
        return scheduler.scheduleTransaction(storeTracksTransaction(Arrays.asList(track)));
    }

    public Observable<TxnResult> storeTracksAsync(final Collection<ApiTrack> tracks) {
        return scheduler.scheduleTransaction(storeTracksTransaction(tracks));
    }

    private PropellerDatabase.Transaction storeTracksTransaction(final Collection<ApiTrack> tracks) {
        return new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (ApiTrack track : tracks) {
                    step(propeller.upsert(Table.USERS.name, TableColumns.Users._ID, buildUserContentValues(track.getUser())));
                    step(propeller.upsert(Table.SOUNDS.name, TableColumns.Sounds._ID, buildTrackContentValues(track)));
                }
            }
        };
    }

    private ContentValues buildTrackContentValues(ApiTrack track) {
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

    private ContentValues buildUserContentValues(ApiUser user) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Users._ID, user.getId())
                .put(TableColumns.Users.USERNAME, user.getUsername())
                .get();
    }

}
