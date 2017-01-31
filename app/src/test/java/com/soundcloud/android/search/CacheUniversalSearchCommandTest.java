package com.soundcloud.android.search;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CacheUniversalSearchCommandTest {

    private CacheUniversalSearchCommand command;

    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private StoreUsersCommand storeUsersCommand;

    @Before
    public void setup() {
        command = new CacheUniversalSearchCommand(storeTracksCommand, storePlaylistsCommand, storeUsersCommand);
    }

    @Test
    public void shouldCacheTrackIfResultItemIsTrack() throws Exception {
        final ApiTrack track = new ApiTrack();
        command.call(singletonList(new ApiUniversalSearchItem(null, null, track)));

        verify(storeTracksCommand).call(singletonList(track));
        verifyZeroInteractions(storePlaylistsCommand);
        verifyZeroInteractions(storeUsersCommand);
    }

    @Test
    public void shouldCachePlaylistIfResultItemIsPlaylist() throws Exception {
        final ApiPlaylist playlist = new ApiPlaylist();
        command.call(singletonList(new ApiUniversalSearchItem(null, playlist, null)));

        verify(storePlaylistsCommand).call(singletonList(playlist));
        verifyZeroInteractions(storeTracksCommand);
        verifyZeroInteractions(storeUsersCommand);
    }

    @Test
    public void shouldCacheUserIfResultItemIsUser() throws Exception {
        final ApiUser user = new ApiUser();
        command.call(singletonList(new ApiUniversalSearchItem(user, null, null)));

        verify(storeUsersCommand).call(singletonList(user));
        verifyZeroInteractions(storeTracksCommand);
        verifyZeroInteractions(storePlaylistsCommand);
    }
}
