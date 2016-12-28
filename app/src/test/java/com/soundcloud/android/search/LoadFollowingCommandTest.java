package com.soundcloud.android.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

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
        Map<Urn, Boolean> userFollowings = command.call(getUserList());
        boolean followed = userFollowings.get(followedUser.getUrn());

        assertThat(followed).isTrue();
        assertThat(userFollowings.get(unfollowedUser.getUrn())).isNull();
    }

    private Iterable<Urn> getUserList() {
        return Arrays.asList(followedUser.getUrn(), unfollowedUser.getUrn());
    }
}
