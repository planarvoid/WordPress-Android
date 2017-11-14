package com.soundcloud.android.sync.activities;

import static com.soundcloud.android.storage.Table.Activities;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithUserFollow;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.comments.StoreCommentCommand;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.UserFixtures;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.tracks.TrackStorage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ReplaceActivitiesCommandTest extends StorageIntegrationTest {

    @Mock private TrackStorage trackStorage;
    private ReplaceActivitiesCommand command;

    @Before
    public void setUp() throws Exception {
        final StoreUsersCommand storeUsersCommand = new StoreUsersCommand(propeller());
        final StoreTracksCommand storeTracksCommand = new StoreTracksCommand(propeller(), storeUsersCommand);
        doAnswer(invocationOnMock -> storeTracksCommand.call((Iterable<? extends TrackRecord>) invocationOnMock.getArguments()[0])).when(trackStorage).asyncStoreTracks(any());
        command = new ReplaceActivitiesCommand(propeller(),
                                               storeUsersCommand,
                                               trackStorage,
                                               new StorePlaylistsCommand(propeller(), storeUsersCommand),
                                               new StoreCommentCommand(propeller()));
    }

    @Test
    public void shouldClearActivitiesTableBeforeInsertingNewItems() {
        ApiUserFollowActivity replacedActivity1 = apiActivityWithUserFollow(UserFixtures.apiUser()).userFollow();
        ApiUserFollowActivity replacedActivity2 = apiActivityWithUserFollow(UserFixtures.apiUser()).userFollow();
        ApiActivityItem storedItem = apiActivityWithUserFollow(UserFixtures.apiUser());
        ApiUserFollowActivity storedActivity = storedItem.userFollow();
        testFixtures().insertUserFollowActivity(replacedActivity1);
        testFixtures().insertUserFollowActivity(replacedActivity2);
        assertThat(select(from(Activities))).counts(2);

        command.call(singleton(storedItem));

        assertThat(select(from(Activities))).counts(1);
        databaseAssertions().assertFollowActivityInserted(storedActivity.getUserUrn(), storedActivity.getCreatedAt());
    }
}
