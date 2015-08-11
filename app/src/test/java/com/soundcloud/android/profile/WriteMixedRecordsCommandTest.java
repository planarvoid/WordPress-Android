package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
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

    @Mock StoreTracksCommand storeTracksCommand;
    @Mock StorePlaylistsCommand storePlaylistsCommand;
    @Mock StoreUsersCommand storeUsersCommand;

    @Mock InsertResult unsuccessfulResult;

    @Before
    public void setUp() throws Exception {
        command = new WriteMixedRecordsCommand(storeTracksCommand, storePlaylistsCommand, storeUsersCommand);
    }

    @Test
    public void writesTracksSuccessfully() {
        final List<ApiTrack> apiTracks = Arrays.asList(ModelFixtures.create(ApiTrack.class));
        when(storeTracksCommand.call(apiTracks)).thenReturn(new InsertResult(1));

        assertThat(command.call(new ModelCollection<>(apiTracks))).isTrue();

        verify(storeTracksCommand).call(apiTracks);
    }

    @Test
    public void writesUsersSuccessfully() {
        final List<ApiUser> apiUsers = Arrays.asList(ModelFixtures.create(ApiUser.class));
        when(storeUsersCommand.call(apiUsers)).thenReturn(new InsertResult(1));

        assertThat(command.call(new ModelCollection<>(apiUsers))).isTrue();

        verify(storeUsersCommand).call(apiUsers);
    }

    @Test
    public void writesPlaylistsSuccessfully() {
        final List<ApiPlaylist> apiPlaylists = Arrays.asList(ModelFixtures.create(ApiPlaylist.class));
        when(storePlaylistsCommand.call(apiPlaylists)).thenReturn(new InsertResult(1));

        assertThat(command.call(new ModelCollection<>(apiPlaylists))).isTrue();

        verify(storePlaylistsCommand).call(apiPlaylists);
    }

    @Test
    public void writesMixedCollectionSuccessfully() {
        final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        final List<PropertySetSource> apiPlaylists = Arrays.asList(apiPlaylist,apiTrack,apiUser);

        when(storePlaylistsCommand.call(Arrays.asList(apiPlaylist))).thenReturn(new InsertResult(1));
        when(storeTracksCommand.call(Arrays.asList(apiTrack))).thenReturn(new InsertResult(1));
        when(storeUsersCommand.call(Arrays.asList(apiUser))).thenReturn(new InsertResult(1));

        assertThat(command.call(new ModelCollection<>(apiPlaylists))).isTrue();

        verify(storePlaylistsCommand).call(Arrays.asList(apiPlaylist));
        verify(storeTracksCommand).call(Arrays.asList(apiTrack));
        verify(storeUsersCommand).call(Arrays.asList(apiUser));
    }

    @Test
    public void returnsFalseIftrackFailsToWrite() {
        final List<ApiTrack> apiTracks = Arrays.asList(ModelFixtures.create(ApiTrack.class));
        when(storeTracksCommand.call(apiTracks)).thenReturn(unsuccessfulResult);

        assertThat(command.call(new ModelCollection<>(apiTracks))).isFalse();
    }

    @Test
    public void returnsFalseIfUsersFailsToWrite() {
        final List<ApiUser> apiUsers = Arrays.asList(ModelFixtures.create(ApiUser.class));
        when(storeUsersCommand.call(apiUsers)).thenReturn(unsuccessfulResult);

        assertThat(command.call(new ModelCollection<>(apiUsers))).isFalse();
    }

    @Test
    public void returnsFalseIfPlaylistFailsToWrite() {
        final List<ApiPlaylist> apiPlaylists = Arrays.asList(ModelFixtures.create(ApiPlaylist.class));
        when(storePlaylistsCommand.call(apiPlaylists)).thenReturn(unsuccessfulResult);

        assertThat(command.call(new ModelCollection<>(apiPlaylists))).isFalse();
    }

    @Test
    public void returnsFalseIfMixedCollectionFailsToWriteOneType() {
        final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        final List<PropertySetSource> apiPlaylists = Arrays.asList(apiPlaylist,apiTrack,apiUser);

        when(storePlaylistsCommand.call(Arrays.asList(apiPlaylist))).thenReturn(new InsertResult(1));
        when(storeTracksCommand.call(Arrays.asList(apiTrack))).thenReturn(new InsertResult(1));
        when(storeUsersCommand.call(Arrays.asList(apiUser))).thenReturn(unsuccessfulResult);

        assertThat(command.call(new ModelCollection<>(apiPlaylists))).isFalse();
    }
}