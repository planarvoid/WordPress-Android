package com.soundcloud.android.users;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class UserStorageTest  extends StorageIntegrationTest {

    private UserStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new UserStorage(propellerRx());
    }

    @Test
    public void loadsUser() {
        ApiUser apiUser = testFixtures().insertUser();

        PropertySet user = storage.loadUser(apiUser.getUrn()).toBlocking().single();

        expect(user).toEqual(getApiUserProperties(apiUser)
                .put(UserProperty.IS_FOLLOWED_BY_ME, false));
    }

    @Test
    public void loadsUserWithoutCountry() {
        ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        apiUser.setCountry(null);
        testFixtures().insertUser(apiUser);

        PropertySet user = storage.loadUser(apiUser.getUrn()).toBlocking().single();

        expect(user).toEqual(getBaseApiUserProperties(apiUser)
                .put(UserProperty.IS_FOLLOWED_BY_ME, false));
    }

    @Test
    public void loadsFollowedUser() {
        ApiUser apiUser = testFixtures().insertUser();
        testFixtures().insertFollowing(apiUser.getUrn());

        PropertySet user = storage.loadUser(apiUser.getUrn()).toBlocking().single();

        expect(user).toEqual(getApiUserProperties(apiUser)
                .put(UserProperty.IS_FOLLOWED_BY_ME, true));
    }

    @Test
    public void loadsUnfollowedUserPending() {
        ApiUser apiUser = testFixtures().insertUser();
        testFixtures().insertFollowingPendingRemoval(apiUser.getUrn());

        PropertySet user = storage.loadUser(apiUser.getUrn()).toBlocking().single();

        expect(user).toEqual(getApiUserProperties(apiUser)
                .put(UserProperty.IS_FOLLOWED_BY_ME, false));
    }

    private PropertySet getApiUserProperties(ApiUser apiUser) {
        return getBaseApiUserProperties(apiUser).put(UserProperty.COUNTRY, apiUser.getCountry());
    }

    private PropertySet getBaseApiUserProperties(ApiUser apiUser) {
        return PropertySet.from(
                UserProperty.URN.bind(apiUser.getUrn()),
                UserProperty.USERNAME.bind(apiUser.getUsername()),
                UserProperty.FOLLOWERS_COUNT.bind(apiUser.getFollowersCount())
        );
    }
}