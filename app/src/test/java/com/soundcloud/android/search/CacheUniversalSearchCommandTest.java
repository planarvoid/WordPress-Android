package com.soundcloud.android.search;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.android.testsupport.TrackFixtures;
import com.soundcloud.android.testsupport.UserFixtures;
import com.soundcloud.android.tracks.TrackRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CacheUniversalSearchCommandTest {

    private CacheUniversalSearchCommand command;

    @Mock private TrackRepository trackRepository;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private StoreUsersCommand storeUsersCommand;

    @Before
    public void setup() {
        command = new CacheUniversalSearchCommand(trackRepository, storePlaylistsCommand, storeUsersCommand);
    }

    @Test
    public void shouldCacheTrackIfResultItemIsTrack() throws Exception {
        final ApiTrack track = TrackFixtures.apiTrack();
        command.call(singletonList(new ApiUniversalSearchItem(null, null, track)));

        verify(trackRepository).storeTracks(singletonList(track));
        verifyZeroInteractions(storePlaylistsCommand);
        verifyZeroInteractions(storeUsersCommand);
    }

    @Test
    public void shouldCachePlaylistIfResultItemIsPlaylist() throws Exception {
        final ApiPlaylist playlist = PlaylistFixtures.apiPlaylist();
        command.call(singletonList(new ApiUniversalSearchItem(null, playlist, null)));

        verify(storePlaylistsCommand).call(singletonList(playlist));
        verifyZeroInteractions(trackRepository);
        verifyZeroInteractions(storeUsersCommand);
    }

    @Test
    public void shouldCacheUserIfResultItemIsUser() throws Exception {
        final ApiUser user = UserFixtures.apiUser();
        command.call(singletonList(new ApiUniversalSearchItem(user, null, null)));

        verify(storeUsersCommand).call(singletonList(user));
        verifyZeroInteractions(trackRepository);
        verifyZeroInteractions(storePlaylistsCommand);
    }
}
