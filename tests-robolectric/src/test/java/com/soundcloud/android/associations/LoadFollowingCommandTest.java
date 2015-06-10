package com.soundcloud.android.associations;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class LoadFollowingCommandTest extends StorageIntegrationTest {

    private LoadFollowingCommand command;
    private ApiUser followedUser;
    private ApiUser unfollowedUser;

    @Before
    public void setUp() throws Exception {
        command = new LoadFollowingCommand(propeller());
        followedUser = testFixtures().insertUser();
        unfollowedUser = testFixtures().insertUser();
        testFixtures().insertFollowing(followedUser.getUrn());
    }

    @Test
    public void shouldAddFollowings() throws Exception {
        Map<Urn, PropertySet> userFollowings = command.call(getUserList());
        PropertySet followedPropertySet = userFollowings.get(followedUser.getUrn());

        expect(followedPropertySet.get(UserProperty.IS_FOLLOWED_BY_ME)).toBe(true);
        expect(userFollowings.get(unfollowedUser.getUrn())).toBeNull();
    }

    private Iterable<PropertySet> getUserList() {
        return Arrays.asList(followedUser.toPropertySet(), unfollowedUser.toPropertySet());
    }
}
