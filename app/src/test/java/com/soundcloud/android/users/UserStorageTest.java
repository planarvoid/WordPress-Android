package com.soundcloud.android.users;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;

public class UserStorageTest  extends StorageIntegrationTest {

    private static final String DESCRIPTION = "description";
    private static final String WEBSITE_URL = "websiteUrl";
    private static final String WEBSITE_NAME = "websiteTitle";
    private static final String DISCOGS_NAME = "discogsName";
    private static final String MYSPACE_NAME = "myspaceName";
    private UserStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new UserStorage(propellerRx());
    }

    @Test
    public void loadsUser() {
        ApiUser apiUser = testFixtures().insertUser();

        PropertySet user = storage.loadUser(apiUser.getUrn()).toBlocking().single();

        assertThat(user).isEqualTo(getApiUserProperties(apiUser)
                .put(UserProperty.IS_FOLLOWED_BY_ME, false));
    }

    @Test
    public void loadsUserWithoutCountry() {
        ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        apiUser.setCountry(null);
        testFixtures().insertUser(apiUser);

        PropertySet user = storage.loadUser(apiUser.getUrn()).toBlocking().single();

        assertThat(user).isEqualTo(getBaseApiUserProperties(apiUser)
                .put(UserProperty.IS_FOLLOWED_BY_ME, false));
    }

    @Test
    public void loadsExtendedUser() {
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        testFixtures().insertExtendedUser(apiUser, DESCRIPTION, WEBSITE_URL, WEBSITE_NAME, DISCOGS_NAME, MYSPACE_NAME);

        PropertySet user = storage.loadUser(apiUser.getUrn()).toBlocking().single();

        assertThat(user).isEqualTo(getExtendedUserProperties(apiUser)
                .put(UserProperty.IS_FOLLOWED_BY_ME, false));
    }

    @Test
    public void loadsFollowedUser() {
        ApiUser apiUser = testFixtures().insertUser();
        testFixtures().insertFollowing(apiUser.getUrn());

        PropertySet user = storage.loadUser(apiUser.getUrn()).toBlocking().single();

        assertThat(user).isEqualTo(getApiUserProperties(apiUser)
                .put(UserProperty.IS_FOLLOWED_BY_ME, true));
    }

    @Test
    public void loadsUnfollowedUserPending() {
        ApiUser apiUser = testFixtures().insertUser();
        testFixtures().insertFollowingPendingRemoval(apiUser.getUrn(), 123);

        PropertySet user = storage.loadUser(apiUser.getUrn()).toBlocking().single();

        assertThat(user).isEqualTo(getApiUserProperties(apiUser)
                .put(UserProperty.IS_FOLLOWED_BY_ME, false));
    }

    private PropertySet getExtendedUserProperties(ApiUser apiUser) {
        return getBaseApiUserProperties(apiUser)
                .put(UserProperty.COUNTRY, apiUser.getCountry())
                .put(UserProperty.DESCRIPTION, DESCRIPTION)
                .put(UserProperty.WEBSITE_URL, WEBSITE_URL)
                .put(UserProperty.WEBSITE_NAME, WEBSITE_NAME)
                .put(UserProperty.DISCOGS_NAME, DISCOGS_NAME)
                .put(UserProperty.MYSPACE_NAME, MYSPACE_NAME);
    }

    private PropertySet getApiUserProperties(ApiUser apiUser) {
        return getBaseApiUserProperties(apiUser)
                .put(UserProperty.COUNTRY, apiUser.getCountry());
    }

    private PropertySet getBaseApiUserProperties(ApiUser apiUser) {
        return PropertySet.from(
                UserProperty.URN.bind(apiUser.getUrn()),
                UserProperty.USERNAME.bind(apiUser.getUsername()),
                UserProperty.FOLLOWERS_COUNT.bind(apiUser.getFollowersCount()),
                UserProperty.IMAGE_URL_TEMPLATE.bind(apiUser.getImageUrlTemplate())
        );
    }
}
