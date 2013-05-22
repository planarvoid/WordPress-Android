package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.util.DatabaseConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@DatabaseConfig.UsingDatabaseMap(DefaultTestRunner.FileDatabaseMap.class)
@RunWith(DefaultTestRunner.class)
public class UserAssociationStorageTest {
    private UserAssociationStorage storage;
    private User user;
    private int initialFollowersCount = 3;

    @Before
    public void initTest() {
        storage = new UserAssociationStorage();
        user = new User(1);
        user.followers_count = initialFollowersCount;
    }

    @Test
    public void shouldStoreFollowingAndNotUpdateFollowersCount() {
        storage.addFollowing(user);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
        expect(TestHelper.reload(user).followers_count).toBe(initialFollowersCount + 1);
    }

    @Test
    public void shouldRemoveFollowingAndUpdateFollowerCount() {
        TestHelper.insertAsUserAssociation(user, UserAssociation.Type.FOLLOWING);
        expect(Content.ME_FOLLOWINGS).toHaveCount(1);
        expect(TestHelper.reload(user).followers_count).toBe(initialFollowersCount);

        storage.removeFollowing(user);
        expect(Content.ME_FOLLOWINGS).toHaveCount(0);
        expect(TestHelper.reload(user).followers_count).toBe(initialFollowersCount - 1);
    }

    @Test
    public void shouldFailToRemoveFollowingAndNotUpdateFollowerCount() {
        TestHelper.insertWithDependencies(user);
        expect(TestHelper.reload(user).followers_count).toBe(initialFollowersCount);

        storage.removeFollowing(user);
        expect(TestHelper.reload(user).followers_count).toBe(initialFollowersCount);
    }

}
