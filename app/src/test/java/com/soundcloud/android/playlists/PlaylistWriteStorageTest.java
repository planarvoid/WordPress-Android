package com.soundcloud.android.playlists;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistWriteStorageTest extends StorageIntegrationTest {

    private PlaylistWriteStorage storage;

    @Mock Observer observer;

    @Before
    public void setup() {
        storage = new PlaylistWriteStorage(testScheduler());
    }

    @Test
    public void shouldStoreApiMobilePlaylistCollection() {
        final List<ApiPlaylist> apiPlaylists = ModelFixtures.create(ApiPlaylist.class, 2);
        storage.storePlaylistsAsync(apiPlaylists).subscribe(observer);

        expectPlaylistInserted(apiPlaylists.get(0));
        expectPlaylistInserted(apiPlaylists.get(1));
    }

    @Test
    public void shouldStorePlaylistsUsingUpsert() {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        playlist.setTitle("new title");

        storage.storePlaylistsAsync(Arrays.asList(playlist)).subscribe(observer);

        assertThat(select(from(Table.SOUNDS.name)), counts(1));
        expectPlaylistInserted(playlist);
    }

    @Test
    public void shouldStoreUsersFromApiMobilePlaylistCollection() {
        final List<ApiPlaylist> apiPlaylists = ModelFixtures.create(ApiPlaylist.class, 2);
        storage.storePlaylistsAsync(apiPlaylists).subscribe(observer);

        expectUserInserted(apiPlaylists.get(0).getUser());
        expectUserInserted(apiPlaylists.get(1).getUser());
    }

    private void expectPlaylistInserted(ApiPlaylist playlist) {
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
                        .whereEq(TableColumns.Sounds.TAG_LIST, TextUtils.join(" ", playlist.getTags()))
        ), counts(1));
    }

    private void expectUserInserted(ApiUser user) {
        assertThat(select(from(Table.SOUND_VIEW.name)
                        .whereEq(TableColumns.SoundView.USER_ID, user.getId())
                        .whereEq(TableColumns.SoundView.USERNAME, user.getUsername())
        ), counts(1));
    }

}