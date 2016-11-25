package com.soundcloud.android.commands;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Collections.singletonList;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.storage.Tables;
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

        command.call(singletonList(user));

        assertThat(select(from(Tables.Users.TABLE))).counts(1);
        databaseAssertions().assertUserInserted(user);
    }
}
