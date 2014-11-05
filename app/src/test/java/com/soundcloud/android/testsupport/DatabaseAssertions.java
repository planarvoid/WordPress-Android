package com.soundcloud.android.testsupport;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.test.matchers.QueryBinding;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.List;

public class DatabaseAssertions {

    private SQLiteDatabase database;

    public DatabaseAssertions(SQLiteDatabase database) {
        this.database = database;
    }

    public void assertTrackWithUserInserted(ApiTrack track) {
        assertTrackInserted(track);
        assertPlayableUserInserted(track.getUser());
    }

    public void assertPlaylistWithUserInserted(ApiPlaylist playlist) {
        assertPlaylistInserted(playlist);
        assertPlayableUserInserted(playlist.getUser());
    }

    public void assertTrackInserted(ApiTrack track) {
        assertThat(select(from(Table.SOUNDS.name)
                .whereEq(TableColumns.Sounds._ID, track.getId())
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .whereEq(TableColumns.Sounds.TITLE, track.getTitle())
                .whereEq(TableColumns.Sounds.DURATION, track.getDuration())
                .whereEq(TableColumns.Sounds.WAVEFORM_URL, track.getWaveformUrl())
                .whereEq(TableColumns.Sounds.STREAM_URL, track.getStreamUrl())
                .whereEq(TableColumns.Sounds.PERMALINK_URL, track.getPermalinkUrl())
                .whereEq(TableColumns.Sounds.CREATED_AT, track.getCreatedAt().getTime())
                .whereEq(TableColumns.Sounds.GENRE, track.getGenre())
                .whereEq(TableColumns.Sounds.SHARING, track.getSharing().value())
                .whereEq(TableColumns.Sounds.USER_ID, track.getUser().getId())
                .whereEq(TableColumns.Sounds.COMMENTABLE, track.isCommentable())
                .whereEq(TableColumns.Sounds.MONETIZABLE, track.isMonetizable())
                .whereEq(TableColumns.Sounds.LIKES_COUNT, track.getStats().getLikesCount())
                .whereEq(TableColumns.Sounds.REPOSTS_COUNT, track.getStats().getRepostsCount())
                .whereEq(TableColumns.Sounds.PLAYBACK_COUNT, track.getStats().getPlaybackCount())
                .whereEq(TableColumns.Sounds.COMMENT_COUNT, track.getStats().getCommentsCount())), counts(1));
    }

    public void assertPlayableUserInserted(ApiUser user) {
        assertThat(select(from(Table.SOUND_VIEW.name)
                        .whereEq(TableColumns.SoundView.USER_ID, user.getId())
                        .whereEq(TableColumns.SoundView.USERNAME, user.getUsername())
        ), counts(1));
    }

    public void assertPlaylistsInserted(List<ApiPlaylist> playlists){
        for (ApiPlaylist playlist : playlists) {
            assertPlaylistInserted(playlist);
        }
    }

    public void assertPlaylistInserted(ApiPlaylist playlist) {
        assertThat(select(from(Table.SOUNDS.name)
                .whereEq(TableColumns.Sounds._ID, playlist.getId())
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(TableColumns.Sounds.TITLE, playlist.getTitle())
                .whereEq(TableColumns.Sounds.DURATION, playlist.getDuration())
                .whereEq(TableColumns.Sounds.CREATED_AT, playlist.getCreatedAt().getTime())
                .whereEq(TableColumns.Sounds.SHARING, playlist.getSharing().value())
                .whereEq(TableColumns.Sounds.USER_ID, playlist.getUser().getId())
                .whereEq(TableColumns.Sounds.LIKES_COUNT, playlist.getStats().getLikesCount())
                .whereEq(TableColumns.Sounds.REPOSTS_COUNT, playlist.getStats().getRepostsCount())
                .whereEq(TableColumns.Sounds.TRACK_COUNT, playlist.getTrackCount())
                .whereEq(TableColumns.Sounds.TAG_LIST, TextUtils.join(" ", playlist.getTags()))), counts(1));
    }

    protected QueryBinding select(Query query) {
        return new QueryBinding(this.database, query);
    }

}
