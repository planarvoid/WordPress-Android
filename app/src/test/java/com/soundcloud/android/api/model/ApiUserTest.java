package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.propeller.PropertySet;
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
    public void shouldTurnToPropertySet() {
        ApiUser user = ModelFixtures.create(ApiUser.class);

        PropertySet propertySet = user.toPropertySet();
        assertThat(propertySet.get(UserProperty.URN)).isEqualTo(user.getUrn());
        assertThat(propertySet.get(UserProperty.USERNAME)).isEqualTo(user.getUsername());
        assertThat(propertySet.get(UserProperty.COUNTRY)).isEqualTo(user.getCountry());
        assertThat(propertySet.get(UserProperty.FOLLOWERS_COUNT)).isEqualTo(user.getFollowersCount());
    }
}
