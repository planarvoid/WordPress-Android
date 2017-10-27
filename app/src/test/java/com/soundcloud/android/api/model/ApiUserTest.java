package com.soundcloud.android.api.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.testsupport.UserFixtures;
import com.soundcloud.android.users.UserItem;
import org.junit.Test;

public class ApiUserTest {

    @Test
    public void shouldDefineEqualityBasedOnUrn() {
        ApiUser apiUser1 = UserFixtures.apiUser();
        ApiUser apiUser2 = UserFixtures.apiUser(apiUser1.getUrn());

        assertThat(apiUser1).isEqualTo(apiUser2);
    }

    @Test
    public void shouldDefineHashCodeBasedOnUrn() {
        ApiUser apiUser1 = UserFixtures.apiUser();
        ApiUser apiUser2 = UserFixtures.apiUser(apiUser1.getUrn());

        assertThat(apiUser1.hashCode()).isEqualTo(apiUser2.hashCode());
    }

    @Test
    public void shouldConvertToUserItem() {
        ApiUser apiUser = UserFixtures.apiUser();

        UserItem userItem = UserFixtures.userItem(apiUser);
        assertThat(userItem.getUrn()).isEqualTo(apiUser.getUrn());
        assertThat(userItem.name()).isEqualTo(apiUser.getUsername());
        assertThat(userItem.country().get()).isEqualTo(apiUser.getCountry());
        assertThat(userItem.followersCount()).isEqualTo(apiUser.getFollowersCount());
    }
}
