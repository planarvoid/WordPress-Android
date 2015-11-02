package com.soundcloud.android.sync.activities;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithLikedPlaylist;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithLikedTrack;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithRepostedPlaylist;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithRepostedTrack;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithTrackComment;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithUser;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithoutPlaylist;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithoutTrack;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiComment;
import static com.soundcloud.propeller.query.Query.from;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.comments.ApiComment;
import com.soundcloud.android.comments.StoreCommentCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.Comments;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserRecord;
import org.junit.Before;
import org.junit.Test;

public class StoreActivitiesCommandTest extends StorageIntegrationTest {

    private StoreActivitiesCommand command;

    @Before
    public void setUp() throws Exception {
        command = new StoreActivitiesCommand(propeller(), new StoreCommentCommand(propeller()));
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

    @Test
    public void shouldStoreCommentFromTrackCommentActivity() {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        ApiUser commenter = ModelFixtures.create(ApiUser.class);
        ApiComment comment = apiComment(Urn.forComment(123), track, commenter);
        ApiActivityItem commentedTrack = apiActivityWithTrackComment(comment);
        ApiTrackCommentActivity commentActivity = commentedTrack.trackComment();

        command.call(singletonList(commentedTrack));

        databaseAssertions().assertCommentInserted(comment);
        final Long commentId = propeller()
                .query(from(Comments.TABLE).select(Comments._ID))
                .firstOrDefault(Long.class, null);
        assertThat(commentId).isNotNull();
        databaseAssertions().assertCommentActivityInserted(commentId,
                track.getUrn(), commenter.getUrn(), commentActivity.getCreatedAt());
    }
}
