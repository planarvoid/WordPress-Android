package com.soundcloud.android.search;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
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
        command.with(Arrays.asList(new ApiUniversalSearchItem(null, null, track))).call();

        verify(storeTracksCommand).call(Arrays.asList(track));
        verifyZeroInteractions(storePlaylistsCommand);
        verifyZeroInteractions(storeUsersCommand);
    }

    @Test
    public void shouldCachePlaylistIfResultItemIsPlaylist() throws Exception {
        final ApiPlaylist playlist = new ApiPlaylist();
        command.with(Arrays.asList(new ApiUniversalSearchItem(null, playlist, null))).call();

        verify(storePlaylistsCommand).call(Arrays.asList(playlist));
        verifyZeroInteractions(storeTracksCommand);
        verifyZeroInteractions(storeUsersCommand);
    }

    @Test
    public void shouldCacheUserIfResultItemIsUser() throws Exception {
        final ApiUser user = new ApiUser();
        command.with(Arrays.asList(new ApiUniversalSearchItem(user, null, null))).call();

        verify(storeUsersCommand).call(Arrays.asList(user));
        verifyZeroInteractions(storeTracksCommand);
        verifyZeroInteractions(storePlaylistsCommand);
    }
}