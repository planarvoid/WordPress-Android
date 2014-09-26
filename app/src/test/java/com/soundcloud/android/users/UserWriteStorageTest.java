package com.soundcloud.android.users;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class UserWriteStorageTest extends StorageIntegrationTest {

    private UserWriteStorage storage;

    @Mock Observer observer;

    @Before
    public void setup() {
        storage = new UserWriteStorage(testScheduler());
    }

    @Test
    public void shouldStoreApiMobileUserCollection() {
        final List<ApiUser> apiUsers = ModelFixtures.create(ApiUser.class, 2);
        storage.storeUsersAsync(apiUsers).subscribe(observer);

        expectUserInserted(apiUsers.get(0));
        expectUserInserted(apiUsers.get(0));
    }

    private void expectUserInserted(ApiUser user) {
        assertThat(select(from(Table.USERS.name)
                        .whereEq(TableColumns.Users._ID, user.getId())
                        .whereEq(TableColumns.Users.USERNAME, user.getUsername())
                        .whereEq(TableColumns.Users.COUNTRY, user.getCountry())
                        .whereEq(TableColumns.Users.FOLLOWERS_COUNT, user.getFollowersCount())
        ), counts(1));
    }
}