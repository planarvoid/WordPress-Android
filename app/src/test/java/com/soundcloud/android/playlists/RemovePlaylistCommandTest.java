package com.soundcloud.android.playlists;

import static android.provider.BaseColumns._ID;
import static com.nhaarman.mockito_kotlin.MockitoKt.whenever;
import static com.soundcloud.android.storage.TableColumns.Activities.SOUND_ID;
import static com.soundcloud.android.storage.TableColumns.Activities.SOUND_TYPE;
import static com.soundcloud.android.storage.TableColumns.ResourceTable._TYPE;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_PLAYLIST;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.offline.OfflineContentStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.sync.activities.ApiPlaylistRepostActivity;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.query.Query;
import io.reactivex.Completable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class RemovePlaylistCommandTest extends StorageIntegrationTest {

    private RemovePlaylistCommand command;

    @Mock private OfflineContentStorage offlineContentStorage;

    @Before
    public void setup() {
        command = new RemovePlaylistCommand(
                new RemovePlaylistFromDatabaseCommand(propeller()),
                offlineContentStorage);
    }

    @Test
    public void removesPlaylist() {
        testFixtures().insertPlaylist();
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        whenever(offlineContentStorage.removePlaylistsFromOffline(playlist.getUrn())).thenReturn(Completable.complete());

        command.call(playlist.getUrn());

        final Query query = from(Tables.Sounds.TABLE)
                .whereEq(_ID, playlist.getId())
                .whereEq(_TYPE, TYPE_PLAYLIST);

        assertThat(select(query)).isEmpty();
    }

    @Test
    public void removedActivitiesAssociatedWithRemovedPlaylist() {
        final ApiPlaylist playlist = PlaylistFixtures.apiPlaylist();
        final ApiPlaylistRepostActivity apiActivityItem = ModelFixtures.apiPlaylistRepostActivity(playlist);
        testFixtures().insertPlaylistRepostActivity(apiActivityItem);
        whenever(offlineContentStorage.removePlaylistsFromOffline(playlist.getUrn())).thenReturn(Completable.complete());

        command.call(playlist.getUrn());

        final Query query = from(Table.Activities)
                .whereEq(SOUND_ID, playlist.getId())
                .whereEq(SOUND_TYPE, TYPE_PLAYLIST);

        assertThat(select(query)).isEmpty();
    }

    @Test
    public void removedSoundStreamEntryAssociatedWithRemovedPlaylist() {
        final ApiPlaylist playlist = PlaylistFixtures.apiPlaylist();
        testFixtures().insertStreamPlaylistPost(playlist.getId(), 123L);
        whenever(offlineContentStorage.removePlaylistsFromOffline(playlist.getUrn())).thenReturn(Completable.complete());

        command.call(playlist.getUrn());

        final Query query = from(Table.SoundStream)
                .whereEq(TableColumns.SoundStream.SOUND_ID, playlist.getId())
                .whereEq(TableColumns.SoundStream.SOUND_TYPE, TYPE_PLAYLIST);

        assertThat(select(query)).isEmpty();
    }

}
