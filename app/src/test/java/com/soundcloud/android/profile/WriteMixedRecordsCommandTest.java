package com.soundcloud.android.profile;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.android.testsupport.TrackFixtures;
import com.soundcloud.android.testsupport.UserFixtures;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.propeller.InsertResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class WriteMixedRecordsCommandTest {

    private WriteMixedRecordsCommand command;

    @Mock TrackRepository trackRepository;
    @Mock StorePlaylistsCommand storePlaylistsCommand;
    @Mock StoreUsersCommand storeUsersCommand;

    @Mock InsertResult unsuccessfulResult;

    @Before
    public void setUp() throws Exception {
        command = new WriteMixedRecordsCommand(trackRepository, storePlaylistsCommand, storeUsersCommand);
    }

    @Test
    public void writesTracksSuccessfully() {
        final List<ApiTrack> apiTracks = Arrays.asList(TrackFixtures.apiTrack());
        when(trackRepository.storeTracks(apiTracks)).thenReturn(true);

        assertThat(command.call(new ModelCollection<>(apiTracks))).isTrue();

        verify(trackRepository).storeTracks(apiTracks);
    }

    @Test
    public void writesUsersSuccessfully() {
        final List<ApiUser> apiUsers = Arrays.asList(UserFixtures.apiUser());
        when(storeUsersCommand.call(apiUsers)).thenReturn(new InsertResult(1));

        assertThat(command.call(new ModelCollection<>(apiUsers))).isTrue();

        verify(storeUsersCommand).call(apiUsers);
    }

    @Test
    public void writesPlaylistsSuccessfully() {
        final List<ApiPlaylist> apiPlaylists = Arrays.asList(PlaylistFixtures.apiPlaylist());
        when(storePlaylistsCommand.call(apiPlaylists)).thenReturn(new InsertResult(1));

        assertThat(command.call(new ModelCollection<>(apiPlaylists))).isTrue();

        verify(storePlaylistsCommand).call(apiPlaylists);
    }

    @Test
    public void writesMixedCollectionSuccessfully() {
        final ApiPlaylist apiPlaylist = PlaylistFixtures.apiPlaylist();
        final ApiTrack apiTrack = TrackFixtures.apiTrack();
        final ApiUser apiUser = UserFixtures.apiUser();
        final List<ApiEntityHolder> apiPlaylists = Arrays.asList(apiPlaylist, apiTrack, apiUser);

        when(storePlaylistsCommand.call(Arrays.asList(apiPlaylist))).thenReturn(new InsertResult(1));
        when(trackRepository.storeTracks(Arrays.asList(apiTrack))).thenReturn(true);
        when(storeUsersCommand.call(Arrays.asList(apiUser))).thenReturn(new InsertResult(1));

        assertThat(command.call(new ModelCollection<>(apiPlaylists))).isTrue();

        verify(storePlaylistsCommand).call(Arrays.asList(apiPlaylist));
        verify(trackRepository).storeTracks(Arrays.asList(apiTrack));
        verify(storeUsersCommand).call(Arrays.asList(apiUser));
    }

    @Test
    public void returnsFalseIftrackFailsToWrite() {
        final List<ApiTrack> apiTracks = Arrays.asList(TrackFixtures.apiTrack());
        when(trackRepository.storeTracks(apiTracks)).thenReturn(false);

        assertThat(command.call(new ModelCollection<>(apiTracks))).isFalse();
    }

    @Test
    public void returnsFalseIfUsersFailsToWrite() {
        final List<ApiUser> apiUsers = Arrays.asList(UserFixtures.apiUser());
        when(storeUsersCommand.call(apiUsers)).thenReturn(unsuccessfulResult);

        assertThat(command.call(new ModelCollection<>(apiUsers))).isFalse();
    }

    @Test
    public void returnsFalseIfPlaylistFailsToWrite() {
        final List<ApiPlaylist> apiPlaylists = Arrays.asList(PlaylistFixtures.apiPlaylist());
        when(storePlaylistsCommand.call(apiPlaylists)).thenReturn(unsuccessfulResult);

        assertThat(command.call(new ModelCollection<>(apiPlaylists))).isFalse();
    }

    @Test
    public void returnsFalseIfMixedCollectionFailsToWriteOneType() {
        final ApiPlaylist apiPlaylist = PlaylistFixtures.apiPlaylist();
        final ApiTrack apiTrack = TrackFixtures.apiTrack();
        final ApiUser apiUser = UserFixtures.apiUser();
        final List<ApiEntityHolder> apiPlaylists = Arrays.asList(apiPlaylist, apiTrack, apiUser);

        when(storePlaylistsCommand.call(Arrays.asList(apiPlaylist))).thenReturn(new InsertResult(1));
        when(trackRepository.storeTracks(Arrays.asList(apiTrack))).thenReturn(true);
        when(storeUsersCommand.call(Arrays.asList(apiUser))).thenReturn(unsuccessfulResult);

        assertThat(command.call(new ModelCollection<>(apiPlaylists))).isFalse();
    }
}
