package com.soundcloud.android.sync.activities;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithLikedPlaylist;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithLikedTrack;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithRepostedPlaylist;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithRepostedTrack;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithTrackComment;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithUserFollow;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiComment;
import static com.soundcloud.propeller.query.Query.from;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
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
        final StoreUsersCommand storeUsersCommand = new StoreUsersCommand(propeller());
        command = new StoreActivitiesCommand(propeller(),
                                             storeUsersCommand,
                                             new StoreTracksCommand(propeller(), storeUsersCommand),
                                             new StorePlaylistsCommand(propeller(), storeUsersCommand),
                                             new StoreCommentCommand(propeller()));
    }

    @Test
    public void shouldStoreLikeActivityWithDependenciesFromLikedTrack() {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        ApiActivityItem activity = apiActivityWithLikedTrack(track);
        ApiEngagementActivity like = activity.getLike().get();
        UserRecord liker = activity.getUser().get();

        command.call(singleton(activity));

        databaseAssertions().assertUserInserted(liker);
        databaseAssertions().assertTrackInserted(track);
        databaseAssertions().assertLikeActivityInserted(track.getUrn(), liker.getUrn(), like.getCreatedAt());
    }

    @Test
    public void shouldStoreLikeActivityFromLikedPlaylist() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        ApiActivityItem activity = apiActivityWithLikedPlaylist(playlist);
        ApiEngagementActivity like = activity.getLike().get();
        UserRecord liker = activity.getUser().get();

        command.call(singleton(activity));

        databaseAssertions().assertUserInserted(liker);
        databaseAssertions().assertPlaylistInserted(playlist);
        databaseAssertions().assertLikeActivityInserted(playlist.getUrn(), liker.getUrn(), like.getCreatedAt());
    }

    @Test
    public void shouldStoreRepostActivityFromRepostedTrack() {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        ApiActivityItem activity = apiActivityWithRepostedTrack(track);
        ApiEngagementActivity repost = activity.getRepost().get();
        UserRecord reposter = activity.getUser().get();

        command.call(singleton(activity));

        databaseAssertions().assertUserInserted(reposter);
        databaseAssertions().assertTrackInserted(track);
        databaseAssertions().assertRepostActivityInserted(track.getUrn(), reposter.getUrn(), repost.getCreatedAt());
    }

    @Test
    public void shouldStoreRepostActivityFromRepostedPlaylist() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        ApiActivityItem activity = apiActivityWithRepostedPlaylist(playlist);
        ApiEngagementActivity repost = activity.getRepost().get();
        UserRecord reposter = activity.getUser().get();

        command.call(singleton(activity));

        databaseAssertions().assertUserInserted(reposter);
        databaseAssertions().assertPlaylistInserted(playlist);
        databaseAssertions().assertRepostActivityInserted(playlist.getUrn(), reposter.getUrn(), repost.getCreatedAt());
    }

    @Test
    public void shouldStoreTrackCommentActivity() {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        ApiUser commenter = ModelFixtures.create(ApiUser.class);
        ApiComment comment = apiComment(Urn.forComment(123), track.getUrn(), commenter);
        ApiActivityItem activity = apiActivityWithTrackComment(comment, track);
        ApiTrackCommentActivity commentActivity = activity.trackComment();

        command.call(singleton(activity));

        databaseAssertions().assertUserInserted(commenter);
        databaseAssertions().assertTrackInserted(track);
        databaseAssertions().assertCommentInserted(comment);
        long commentId = propeller().query(from(Comments.TABLE).select(Comments._ID)).first(Long.class);
        assertThat(commentId).isNotNull();
        databaseAssertions().assertCommentActivityInserted(commentId,
                                                           track.getUrn(),
                                                           commenter.getUrn(),
                                                           commentActivity.getCreatedAt());
    }

    @Test
    public void shouldStoreUserFollowActivity() {
        ApiUser follower = ModelFixtures.create(ApiUser.class);
        ApiActivityItem activity = apiActivityWithUserFollow(follower);
        ApiUserFollowActivity follow = activity.userFollow();

        command.call(singleton(activity));

        databaseAssertions().assertUserInserted(follower);
        databaseAssertions().assertFollowActivityInserted(follower.getUrn(), follow.getCreatedAt());
    }
}
