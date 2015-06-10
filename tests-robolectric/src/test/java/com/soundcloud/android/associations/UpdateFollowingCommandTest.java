package com.soundcloud.android.associations;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.associations.UpdateFollowingCommand.UpdateFollowingParams;
import static com.soundcloud.propeller.query.Filter.filter;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.ContentValues;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class UpdateFollowingCommandTest extends StorageIntegrationTest {

    private UpdateFollowingCommand command;
    private Urn targetUrn;
    private ApiUser apiUser;

    @Mock AccountOperations accountOperations;

    @Before
    public void setUp() throws Exception {
        command = new UpdateFollowingCommand(propeller(), accountOperations);
        apiUser = testFixtures().insertUser();
        targetUrn = apiUser.getUrn();
    }

    @Test
    public void addsUserAssociation() throws Exception {
        command.call(new UpdateFollowingParams(targetUrn, true));

        databaseAssertions().assertUserFollowingsPending(targetUrn, true);
    }

    @Test
    public void removesUserAssociation() throws Exception {
        command.call(new UpdateFollowingParams(targetUrn, false));

        databaseAssertions().assertUserFollowingsPending(targetUrn, false);
    }

    @Test
    public void incrementsUserFollowers() throws Exception {
        final int numberOfFollowers = command.call(new UpdateFollowingParams(targetUrn, true));

        expect(numberOfFollowers).toBe(apiUser.getFollowersCount() + 1);
        databaseAssertions().assertUserFollowersCount(targetUrn, numberOfFollowers);
    }

    @Test
    public void doesNotIncrementUserFollowersWhenNotFollowing() throws Exception {
        final int numberOfFollowers = command.call(new UpdateFollowingParams(targetUrn, false));

        expect(numberOfFollowers).toBe(apiUser.getFollowersCount());
        databaseAssertions().assertUserFollowersCount(targetUrn, numberOfFollowers);
    }

    @Test
    public void decrementUserFollowersWhenFollowing() throws Exception {
        final int numberOfFollowers = command.call(new UpdateFollowingParams(targetUrn, true));
        databaseAssertions().assertUserFollowersCount(targetUrn, numberOfFollowers);

        command.call(new UpdateFollowingParams(targetUrn, false));
        databaseAssertions().assertUserFollowersCount(targetUrn, numberOfFollowers - 1);
    }
}