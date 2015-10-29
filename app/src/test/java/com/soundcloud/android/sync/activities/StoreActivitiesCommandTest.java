package com.soundcloud.android.sync.activities;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithLikedPlaylist;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithLikedTrack;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithRepostedPlaylist;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithRepostedTrack;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithUser;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithoutPlaylist;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithoutTrack;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserRecord;
import org.junit.Before;
import org.junit.Test;

public class StoreActivitiesCommandTest extends StorageIntegrationTest {

    private StoreActivitiesCommand command;

    @Before
    public void setUp() throws Exception {
        command = new StoreActivitiesCommand(propeller());
    }

    @Test
    public void shouldStoreUsersFromActivityItems() {
        ApiUser user = ModelFixtures.create(ApiUser.class);

        command.call(singletonList(apiActivityWithUser(user)));

        databaseAssertions().assertUserInserted(user);
    }

    @Test
    public void shouldStoreTracksFromActivityItems() {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);

        command.call(asList(apiActivityWithoutTrack(), apiActivityWithLikedTrack(track)));

        databaseAssertions().assertTrackInserted(track);
    }

    @Test
    public void shouldStorePlaylistsFromActivityItems() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);

        command.call(asList(apiActivityWithoutPlaylist(), apiActivityWithLikedPlaylist(playlist)));

        databaseAssertions().assertPlaylistInserted(playlist);
    }

    @Test
    public void shouldStoreLikeActivityFromLikedTrack() {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        ApiActivityItem likedTrack = apiActivityWithLikedTrack(track);
        UserRecord liker = likedTrack.getUser().get();

        command.call(singletonList(likedTrack));

        databaseAssertions().assertLikeActivityInserted(track.getUrn(), liker.getUrn(), likedTrack.getDate());
    }

    @Test
    public void shouldStoreLikeActivityFromLikedPlaylist() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        ApiActivityItem likedPlaylist = apiActivityWithLikedPlaylist(playlist);
        UserRecord liker = likedPlaylist.getUser().get();

        command.call(singletonList(likedPlaylist));

        databaseAssertions().assertLikeActivityInserted(playlist.getUrn(), liker.getUrn(), likedPlaylist.getDate());
    }

    @Test
    public void shouldStoreRepostActivityFromRepostedTrack() {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        ApiActivityItem repostedTrack = apiActivityWithRepostedTrack(track);
        UserRecord reposter = repostedTrack.getUser().get();

        command.call(singletonList(repostedTrack));

        databaseAssertions().assertRepostActivityInserted(track.getUrn(), reposter.getUrn(), repostedTrack.getDate());
    }

    @Test
    public void shouldStoreRepostActivityFromRepostedPlaylist() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        ApiActivityItem repostedPlaylist = apiActivityWithRepostedPlaylist(playlist);
        UserRecord reposter = repostedPlaylist.getUser().get();

        command.call(singletonList(repostedPlaylist));

        databaseAssertions().assertRepostActivityInserted(playlist.getUrn(), reposter.getUrn(), repostedPlaylist.getDate());
    }
}
