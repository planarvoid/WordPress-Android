package com.soundcloud.android.commands;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.TestUserRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class StoreUsersCommandTest extends StorageIntegrationTest {

    private StoreUsersCommand command;

    @Before
    public void setup() {
        command = new StoreUsersCommand(propeller());
    }

    @Test
    public void shouldPersistUsersInDatabase() throws Exception {
        final List<TestUserRecord> users = TestUserRecord.create(2);

        command.call(users);

        databaseAssertions().assertUserInserted(users.get(0));
        databaseAssertions().assertUserInserted(users.get(1));
    }

    @Test
    public void shouldStoreUsersUsingUpsert() throws Exception {
        final ApiUser user = testFixtures().insertUser();
        user.setUsername("new username");

        command.call(Arrays.asList(user));

        assertThat(select(from(Table.Users.name())), counts(1));
        databaseAssertions().assertUserInserted(user);
    }
}