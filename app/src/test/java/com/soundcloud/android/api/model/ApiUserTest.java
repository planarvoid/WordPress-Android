package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserItem;
import org.junit.Test;

public class ApiUserTest extends AndroidUnitTest {

    @Test
    public void shouldDefineEqualityBasedOnUrn() {
        ApiUser apiUser1 = ModelFixtures.create(ApiUser.class);
        ApiUser apiUser2 = ModelFixtures.create(ApiUser.class);
        apiUser2.setUrn(apiUser1.getUrn());

        assertThat(apiUser1).isEqualTo(apiUser2);
    }

    @Test
    public void shouldDefineHashCodeBasedOnUrn() {
        ApiUser apiUser1 = ModelFixtures.create(ApiUser.class);
        ApiUser apiUser2 = ModelFixtures.create(ApiUser.class);
        apiUser2.setUrn(apiUser1.getUrn());

        assertThat(apiUser1.hashCode()).isEqualTo(apiUser2.hashCode());
    }

    @Test
    public void shouldConvertToUserItem() {
        ApiUser apiUser = ModelFixtures.create(ApiUser.class);

        UserItem userItem = UserItem.from(apiUser);
        assertThat(userItem.getUrn()).isEqualTo(apiUser.getUrn());
        assertThat(userItem.getName()).isEqualTo(apiUser.getUsername());
        assertThat(userItem.getCountry().get()).isEqualTo(apiUser.getCountry());
        assertThat(userItem.getFollowersCount()).isEqualTo(apiUser.getFollowersCount());
    }
}
