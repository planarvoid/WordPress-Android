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
        final StoreTracksCommand storeTracksCommand = new StoreTracksCommand(propeller());
        final StorePlaylistsCommand storePlaylistsCommand = new StorePlaylistsCommand(propeller());
        final StoreUsersCommand storeUsersCommand = new StoreUsersCommand(propeller());
        storeProfileCommand = new StoreProfileCommand(storeTracksCommand, storePlaylistsCommand, storeUsersCommand);
    }

    @Test
    public void shouldStoreTheTracksFromTheUsersSpotlight() {
        final ApiPlayableHolder spotlightTrack = ModelFixtures.apiTrackHolder();
        final ModelCollection<ApiPlayableHolder> spotlight = new ModelCollection<>(Collections.singletonList(spotlightTrack));
        final ApiUserProfile profile = new UserProfileFixtures.Builder().spotlight(spotlight).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertTrackInserted(((TrackRecord) spotlightTrack.getItem().get()));
    }

    @Test
    public void shouldStoreThePlaylistsFromTheUsersSpotlight() {
        final ApiPlayableHolder spotlightPlaylist = ModelFixtures.apiPlaylistHolder();
        final ModelCollection<ApiPlayableHolder> spotlight = new ModelCollection<>(Collections.singletonList(spotlightPlaylist));
        final ApiUserProfile profile = new UserProfileFixtures.Builder().spotlight(spotlight).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertPlaylistInserted(((PlaylistRecord) spotlightPlaylist.getItem().get()).getUrn());
    }

    @Test
    public void shouldStoreTheUsersTracks() {
        final ApiTrackPost trackPost = new ApiTrackPost(ModelFixtures.create(ApiTrack.class));
        final ModelCollection<ApiTrackPost> tracks = new ModelCollection<>(Collections.singletonList(trackPost));
        final ApiUserProfile profile = new UserProfileFixtures.Builder().tracks(tracks).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertTrackInserted(trackPost.getTrackRecord());
    }

    @Test
    public void shouldStoreTheUsersReleases() {
        final ApiPlaylistPost release = new ApiPlaylistPost(ModelFixtures.create(ApiPlaylist.class));
        final ModelCollection<ApiPlaylistPost> releases = new ModelCollection<>(Collections.singletonList(release));
        final ApiUserProfile profile = new UserProfileFixtures.Builder().releases(releases).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertPlaylistInserted(release.getPlaylistRecord().getUrn());
    }

    @Test
    public void shouldStoreTheUsersPlaylists() {
        final ApiPlaylistPost playlist = new ApiPlaylistPost(ModelFixtures.create(ApiPlaylist.class));
        final ModelCollection<ApiPlaylistPost> playlists = new ModelCollection<>(Collections.singletonList(playlist));
        final ApiUserProfile profile = new UserProfileFixtures.Builder().playlists(playlists).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertPlaylistInserted(playlist.getPlaylistRecord().getUrn());
    }

    @Test
    public void shouldStoreTheTracksFromTheUsersReposts() {
        final ApiPlayableHolder trackRepost = ModelFixtures.apiTrackHolder();
        final ModelCollection<ApiPlayableHolder> reposts = new ModelCollection<>(Collections.singletonList(trackRepost));
        final ApiUserProfile profile = new UserProfileFixtures.Builder().reposts(reposts).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertTrackInserted(((TrackRecord) trackRepost.getItem().get()));
    }

    @Test
    public void shouldStoreThePlaylistsFromTheUsersReposts() {
        final ApiPlayableHolder playlistRepost = ModelFixtures.apiPlaylistHolder();
        final ModelCollection<ApiPlayableHolder> reposts = new ModelCollection<>(Collections.singletonList(playlistRepost));
        final ApiUserProfile profile = new UserProfileFixtures.Builder().reposts(reposts).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertPlaylistInserted(((PlaylistRecord) playlistRepost.getItem().get()).getUrn());
    }

    @Test
    public void shouldStoreTheTracksFromTheUsersLikes() {
        final ApiPlayableHolder trackLike = ModelFixtures.apiTrackHolder();
        final ModelCollection<ApiPlayableHolder> likes = new ModelCollection<>(Collections.singletonList(trackLike));
        final ApiUserProfile profile = new UserProfileFixtures.Builder().likes(likes).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertTrackInserted(((TrackRecord) trackLike.getItem().get()));
    }

    @Test
    public void shouldStoreThePlaylistsFromTheUsersLikes() {
        final ApiPlayableHolder playlistLike = ModelFixtures.apiPlaylistHolder();
        final ModelCollection<ApiPlayableHolder> likes = new ModelCollection<>(Collections.singletonList(playlistLike));
        final ApiUserProfile profile = new UserProfileFixtures.Builder().likes(likes).build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertPlaylistInserted(((PlaylistRecord) playlistLike.getItem().get()).getUrn());
    }

    @Test
    public void shouldStoreTheUser() {
        final ApiUserProfile profile = new UserProfileFixtures.Builder().build();

        storeProfileCommand.call(profile);

        databaseAssertions().assertUserInserted(profile.getUser());
    }
}
