package com.soundcloud.android.sync.playlists;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.WriteResult;
import io.reactivex.Completable;
import io.reactivex.subjects.CompletableSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.util.Pair;

import java.util.Collections;
import java.util.Date;

public class ReplacePlaylistPostCommandTest extends StorageIntegrationTest {

    @Mock private OfflineContentOperations offlineContentOperations;
    private final CompletableSubject replaceSubject = CompletableSubject.create();
    private ApiPlaylist oldPlaylist;
    private ApiPlaylist newPlaylist;

    private ReplacePlaylistPostCommand command;

    @Before
    public void setUp() throws Exception {
        command = new ReplacePlaylistPostCommand(propeller(), offlineContentOperations);
        oldPlaylist = testFixtures().insertLocalPlaylist();
        newPlaylist = PlaylistFixtures.apiPlaylist();

        when(offlineContentOperations.replaceOfflinePlaylist(oldPlaylist.getUrn(), newPlaylist.getUrn())).thenReturn(Completable.complete());
    }

    @Test
    public void shouldReplaceOldPlaylistMetadata() throws Exception {
        WriteResult result = command.with(Pair.create(oldPlaylist.getUrn(), newPlaylist)).call();
        assertThat(result.success()).isTrue();

        databaseAssertions().assertPlaylistInserted(newPlaylist);
        databaseAssertions().assertPlaylistNotStored(oldPlaylist.getId());
    }

    @Test
    public void shouldUpdatePlaylistTracksTableWithNewPlaylistId() throws Exception {
        ApiTrack playlistTrack = testFixtures().insertPlaylistTrack(oldPlaylist, 0);

        WriteResult result = command.with(Pair.create(oldPlaylist.getUrn(), newPlaylist)).call();
        assertThat(result.success()).isTrue();

        databaseAssertions().assertPlaylistTracklist(newPlaylist.getId(),
                                                     Collections.singletonList(playlistTrack.getUrn()));
    }

    @Test
    public void shouldClearPlaylistTracksAddedAt() throws Exception {
        ApiTrack playlistTrack = testFixtures().insertPlaylistTrackPendingAddition(oldPlaylist, 0, new Date());

        WriteResult result = command.with(Pair.create(oldPlaylist.getUrn(), newPlaylist)).call();
        assertThat(result.success()).isTrue();

        databaseAssertions().assertNoPlaylistTrackForAddition(newPlaylist.getUrn(),
                                                     playlistTrack.getUrn());
    }

    @Test
    public void shouldUpdatePostsTableWithNewPlaylistId() throws Exception {
        Date createdAt = new Date(456L);
        ApiPlaylist updatedPlaylist = newPlaylist.toBuilder().createdAt(createdAt).build();
        testFixtures().insertPlaylistPost(oldPlaylist.getId(), 123L, false);

        WriteResult result = command.with(Pair.create(oldPlaylist.getUrn(), updatedPlaylist)).call();
        assertThat(result.success()).isTrue();

        databaseAssertions().assertPlaylistPostInsertedFor(updatedPlaylist, createdAt);
    }

    @Test
    public void shouldUpdateLikesTableWithNewPlaylistId() throws Exception {
        testFixtures().insertLike(oldPlaylist.getId(), Tables.Sounds.TYPE_PLAYLIST, new Date());

        WriteResult result = command.with(Pair.create(oldPlaylist.getUrn(), newPlaylist)).call();
        assertThat(result.success()).isTrue();

        databaseAssertions().assertPlaylistLikeInsertedFor(newPlaylist);
    }
}
