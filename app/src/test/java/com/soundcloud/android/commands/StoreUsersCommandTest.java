package com.soundcloud.android.commands;

import static com.soundcloud.android.storage.Table.Users;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Arrays.asList;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.TestUserRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

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

        command.call(asList(user));

        assertThat(select(from(Users.name()))).counts(1);
        databaseAssertions().assertUserInserted(user);
    }
}
