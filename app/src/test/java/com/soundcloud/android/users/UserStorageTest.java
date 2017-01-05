package com.soundcloud.android.users;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;

public class UserStorageTest extends StorageIntegrationTest {

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

        assertThat(user).isEqualTo(getApiUserProperties(apiUser));
    }

    @Test
    public void loadsUserWithoutCountryOrCity() {
        ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        apiUser.setCountry(null);
        apiUser.setCity(null);
        testFixtures().insertUser(apiUser);

        PropertySet user = storage.loadUser(apiUser.getUrn()).toBlocking().single();

        assertThat(user).isEqualTo(getBaseApiUserProperties(apiUser));
    }

    @Test
    public void loadsExtendedUser() {
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        testFixtures().insertExtendedUser(apiUser, DESCRIPTION, WEBSITE_URL, WEBSITE_NAME, DISCOGS_NAME, MYSPACE_NAME);

        PropertySet user = storage.loadUser(apiUser.getUrn()).toBlocking().single();

        assertThat(user).isEqualTo(getExtendedUserProperties(apiUser));
    }

    @Test
    public void loadsFollowedUser() {
        ApiUser apiUser = testFixtures().insertUser();
        testFixtures().insertFollowing(apiUser.getUrn());

        PropertySet user = storage.loadUser(apiUser.getUrn()).toBlocking().single();

        assertThat(user).isEqualTo(
                getApiUserProperties(apiUser).put(UserProperty.IS_FOLLOWED_BY_ME, true));
    }

    @Test
    public void loadsUnfollowedUserPending() {
        ApiUser apiUser = testFixtures().insertUser();
        testFixtures().insertFollowingPendingRemoval(apiUser.getUrn(), 123);

        PropertySet user = storage.loadUser(apiUser.getUrn()).toBlocking().single();

        assertThat(user).isEqualTo(getApiUserProperties(apiUser));
    }

    @Test
    public void loadUserWithArtistStation() {
        final Urn artistStation = Urn.forArtistStation(123);
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        testFixtures().insertUser(apiUser, artistStation);

        PropertySet user = storage.loadUser(apiUser.getUrn()).toBlocking().single();

        assertThat(user).isEqualTo(
                getApiUserProperties(apiUser).put(UserProperty.ARTIST_STATION, Optional.of(artistStation)));
    }

    private PropertySet getExtendedUserProperties(ApiUser apiUser) {
        return getBaseApiUserProperties(apiUser)
                .put(UserProperty.COUNTRY, apiUser.getCountry())
                .put(UserProperty.CITY, apiUser.getCity())
                .put(UserProperty.DESCRIPTION, DESCRIPTION)
                .put(UserProperty.WEBSITE_URL, WEBSITE_URL)
                .put(UserProperty.WEBSITE_NAME, WEBSITE_NAME)
                .put(UserProperty.DISCOGS_NAME, DISCOGS_NAME)
                .put(UserProperty.MYSPACE_NAME, MYSPACE_NAME);
    }

    private PropertySet getApiUserProperties(ApiUser apiUser) {
        return getBaseApiUserProperties(apiUser)
                .put(UserProperty.COUNTRY, apiUser.getCountry())
                .put(UserProperty.CITY, apiUser.getCity());
    }

    private PropertySet getBaseApiUserProperties(ApiUser apiUser) {
        return PropertySet.from(
                UserProperty.URN.bind(apiUser.getUrn()),
                UserProperty.USERNAME.bind(apiUser.getUsername()),
                UserProperty.FIRST_NAME.bind(apiUser.getFirstName()),
                UserProperty.LAST_NAME.bind(apiUser.getLastName()),
                UserProperty.SIGNUP_DATE.bind(apiUser.getCreatedAt()),
                UserProperty.FOLLOWERS_COUNT.bind(apiUser.getFollowersCount()),
                UserProperty.IMAGE_URL_TEMPLATE.bind(apiUser.getImageUrlTemplate()),
                UserProperty.IS_FOLLOWED_BY_ME.bind(false),
                UserProperty.VISUAL_URL.bind(Optional.<String>absent()),
                UserProperty.ARTIST_STATION.bind(Optional.<Urn>absent())
        );
    }
}
