package com.soundcloud.android.sync.activities;

import static com.soundcloud.android.storage.Table.Activities;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiActivityWithUserFollow;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.create;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Collections.singleton;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.comments.StoreCommentCommand;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

public class ReplaceActivitiesCommandTest extends StorageIntegrationTest {

    private ReplaceActivitiesCommand command;

    @Before
    public void setUp() throws Exception {
        final StoreUsersCommand storeUsersCommand = new StoreUsersCommand(propeller());
        command = new ReplaceActivitiesCommand(propeller(),
                                               storeUsersCommand,
                                               new StoreTracksCommand(propeller(), storeUsersCommand),
                                               new StorePlaylistsCommand(propeller(), storeUsersCommand),
                                               new StoreCommentCommand(propeller()));
    }

    @Test
    public void shouldClearActivitiesTableBeforeInsertingNewItems() {
        ApiUserFollowActivity replacedActivity1 = apiActivityWithUserFollow(create(ApiUser.class)).userFollow();
        ApiUserFollowActivity replacedActivity2 = apiActivityWithUserFollow(create(ApiUser.class)).userFollow();
        ApiActivityItem storedItem = apiActivityWithUserFollow(create(ApiUser.class));
        ApiUserFollowActivity storedActivity = storedItem.userFollow();
        testFixtures().insertUserFollowActivity(replacedActivity1);
        testFixtures().insertUserFollowActivity(replacedActivity2);
        assertThat(select(from(Activities))).counts(2);

        command.call(singleton(storedItem));

        assertThat(select(from(Activities))).counts(1);
        databaseAssertions().assertFollowActivityInserted(storedActivity.getUserUrn(), storedActivity.getCreatedAt());
    }
}
