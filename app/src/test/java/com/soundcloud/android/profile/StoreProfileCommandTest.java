package com.soundcloud.android.profile;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class StoreProfileCommandTest extends StorageIntegrationTest {
    private StoreProfileCommand storeProfileCommand;

    @Before
    public void setup() {
        final StoreUsersCommand storeUsersCommand = new StoreUsersCommand(propeller());
        final StoreTracksCommand storeTracksCommand = new StoreTracksCommand(propeller(), storeUsersCommand);
        final StorePlaylistsCommand storePlaylistsCommand = new StorePlaylistsCommand(propeller(), storeUsersCommand);
        final WriteMixedRecordsCommand writeMixedRecordsCommand = new WriteMixedRecordsCommand(storeTracksCommand,
                                                                                               storePlaylistsCommand,
                                                                                               storeUsersCommand);

        storeProfileCommand = new StoreProfileCommand(writeMixedRecordsCommand);
    }

    @Test
    public void shouldStoreTheUser() {
        final ApiUserProfile profile = new UserProfileRecordFixtures.Builder().build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertUserInserted(profile.getUser());
    }

    @Test
    public void shouldStoreTheTracksFromTheUsersSpotlight() {
        final ApiPlayableSource spotlightTrack = ModelFixtures.apiTrackHolder();
        final ModelCollection<ApiPlayableSource> spotlight = new ModelCollection<>(Collections.singletonList(
                spotlightTrack));
        final ApiUserProfile profile = new UserProfileRecordFixtures.Builder().spotlight(spotlight).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertTrackInserted(((TrackRecord) spotlightTrack.getEntityHolder().get()));
    }

    @Test
    public void shouldStoreThePlaylistsFromTheUsersSpotlight() {
        final ApiPlayableSource spotlightPlaylist = ModelFixtures.apiPlaylistHolder();
        final ModelCollection<ApiPlayableSource> spotlight = new ModelCollection<>(Collections.singletonList(
                spotlightPlaylist));
        final ApiUserProfile profile = new UserProfileRecordFixtures.Builder().spotlight(spotlight).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertPlaylistInserted(((PlaylistRecord) spotlightPlaylist.getEntityHolder()
                                                                                       .get()).getUrn());
    }

    @Test
    public void shouldStoreTheUsersTracks() {
        final ApiTrackPost trackPost = new ApiTrackPost(ModelFixtures.create(ApiTrack.class));
        final ModelCollection<ApiTrackPost> tracks = new ModelCollection<>(Collections.singletonList(trackPost));
        final ApiUserProfile profile = new UserProfileRecordFixtures.Builder().tracks(tracks).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertTrackInserted(trackPost.getTrackRecord());
    }

    @Test
    public void shouldStoreTheUsersAlbums() {
        final ApiPlaylistPost album = new ApiPlaylistPost(ModelFixtures.create(ApiPlaylist.class));
        final ModelCollection<ApiPlaylistPost> albums = new ModelCollection<>(Collections.singletonList(album));
        final ApiUserProfile profile = new UserProfileRecordFixtures.Builder().albums(albums).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertPlaylistInserted(album.getPlaylistRecord().getUrn());
    }

    @Test
    public void shouldStoreTheUsersPlaylists() {
        final ApiPlaylistPost playlist = new ApiPlaylistPost(ModelFixtures.create(ApiPlaylist.class));
        final ModelCollection<ApiPlaylistPost> playlists = new ModelCollection<>(Collections.singletonList(playlist));
        final ApiUserProfile profile = new UserProfileRecordFixtures.Builder().playlists(playlists).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertPlaylistInserted(playlist.getPlaylistRecord().getUrn());
    }

    @Test
    public void shouldStoreTheTracksFromTheUsersReposts() {
        final ApiPlayableSource trackRepost = ModelFixtures.apiTrackHolder();
        final ModelCollection<ApiPlayableSource> reposts = new ModelCollection<>(Collections.singletonList(trackRepost));
        final ApiUserProfile profile = new UserProfileRecordFixtures.Builder().reposts(reposts).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertTrackInserted(((TrackRecord) trackRepost.getEntityHolder().get()));
    }

    @Test
    public void shouldStoreThePlaylistsFromTheUsersReposts() {
        final ApiPlayableSource playlistRepost = ModelFixtures.apiPlaylistHolder();
        final ModelCollection<ApiPlayableSource> reposts = new ModelCollection<>(Collections.singletonList(
                playlistRepost));
        final ApiUserProfile profile = new UserProfileRecordFixtures.Builder().reposts(reposts).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertPlaylistInserted(((PlaylistRecord) playlistRepost.getEntityHolder().get()).getUrn());
    }

    @Test
    public void shouldStoreTheTracksFromTheUsersLikes() {
        final ApiPlayableSource trackLike = ModelFixtures.apiTrackHolder();
        final ModelCollection<ApiPlayableSource> likes = new ModelCollection<>(Collections.singletonList(trackLike));
        final ApiUserProfile profile = new UserProfileRecordFixtures.Builder().likes(likes).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertTrackInserted(((TrackRecord) trackLike.getEntityHolder().get()));
    }

    @Test
    public void shouldStoreThePlaylistsFromTheUsersLikes() {
        final ApiPlayableSource playlistLike = ModelFixtures.apiPlaylistHolder();
        final ModelCollection<ApiPlayableSource> likes = new ModelCollection<>(Collections.singletonList(playlistLike));
        final ApiUserProfile profile = new UserProfileRecordFixtures.Builder().likes(likes).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertPlaylistInserted(((PlaylistRecord) playlistLike.getEntityHolder().get()).getUrn());
    }
}
