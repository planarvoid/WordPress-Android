package com.soundcloud.android.associations;

import static com.soundcloud.android.associations.UpdateFollowingCommand.UpdateFollowingParams;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class UpdateFollowingCommandTest extends StorageIntegrationTest {

    private UpdateFollowingCommand command;
    private Urn targetUrn;
    private ApiUser apiUser;

    @Mock AccountOperations accountOperations;

    @Before
    public void setUp() {
        command = new UpdateFollowingCommand(propeller(), accountOperations);
        apiUser = testFixtures().insertUser();
        targetUrn = apiUser.getUrn();
    }

    @Test
    public void addsUserAssociation() {
        command.call(new UpdateFollowingParams(targetUrn, true));

        databaseAssertions().assertUserFollowingsPending(targetUrn, true);
    }

    @Test
    public void removesUserAssociation() {
        command.call(new UpdateFollowingParams(targetUrn, false));

        databaseAssertions().assertUserFollowingsPending(targetUrn, false);
    }

    @Test
    public void followIncrementsUserFollowers() {
        final int numberOfFollowers = command.call(new UpdateFollowingParams(targetUrn, true));

        assertThat(numberOfFollowers).isEqualTo(apiUser.getFollowersCount() + 1);
        databaseAssertions().assertUserFollowersCount(targetUrn, numberOfFollowers);
    }

    @Test
    public void followDoesNotIncrementUserFollowersWhenFollowing() {
        testFixtures().insertFollowing(targetUrn);

        final int numberOfFollowers = command.call(new UpdateFollowingParams(targetUrn, true));

        assertThat(numberOfFollowers).isEqualTo(apiUser.getFollowersCount());
        databaseAssertions().assertUserFollowersCount(targetUrn, numberOfFollowers);
    }

    @Test
    public void unfollowDoesNotIncrementUserFollowersWhenNotFollowing() {
        final int numberOfFollowers = command.call(new UpdateFollowingParams(targetUrn, false));

        assertThat(numberOfFollowers).isEqualTo(apiUser.getFollowersCount());
        databaseAssertions().assertUserFollowersCount(targetUrn, numberOfFollowers);
    }

    @Test
    public void unfollowDecrementsUserFollowersWhenFollowing() {
        final int numberOfFollowers = command.call(new UpdateFollowingParams(targetUrn, true));
        databaseAssertions().assertUserFollowersCount(targetUrn, numberOfFollowers);

        command.call(new UpdateFollowingParams(targetUrn, false));
        databaseAssertions().assertUserFollowersCount(targetUrn, numberOfFollowers - 1);
    }

    @Test
    public void followDoesNotIncrementUserFollowersWhenCountIsUnknown() {
        ApiUser apiUser = insertUserWithUnknownFollowerCount();
        targetUrn = apiUser.getUrn();

        final int numberOfFollowers = command.call(new UpdateFollowingParams(targetUrn, true));

        assertThat(numberOfFollowers).isEqualTo(apiUser.getFollowersCount());
        databaseAssertions().assertUserFollowersCount(targetUrn, numberOfFollowers);
    }

    @Test
    public void unfollowDoesNotDecrementUserFollowersWhenCountIsUnknown() {
        ApiUser apiUser = insertUserWithUnknownFollowerCount();
        targetUrn = apiUser.getUrn();
        testFixtures().insertFollowing(targetUrn);

        final int numberOfFollowers = command.call(new UpdateFollowingParams(targetUrn, false));

        assertThat(numberOfFollowers).isEqualTo(apiUser.getFollowersCount());
        databaseAssertions().assertUserFollowersCount(targetUrn, numberOfFollowers);
    }

    private ApiUser insertUserWithUnknownFollowerCount() {
        ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        apiUser.setFollowersCount(-1);
        testFixtures().insertUser(apiUser);
        return apiUser;
    }
}
