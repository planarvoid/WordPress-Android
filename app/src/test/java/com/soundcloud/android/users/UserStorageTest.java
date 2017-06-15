package com.soundcloud.android.users;

import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.fromNullable;
import static com.soundcloud.java.optional.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
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
        storage = new UserStorage(propellerRxV2());
    }

    @Test
    public void loadsUser() {
        ApiUser apiUser = testFixtures().insertUser();

        User user = storage.loadUser(apiUser.getUrn()).blockingGet();

        assertThat(user).isEqualTo(getApiUserBuilder(apiUser).build());
    }

    @Test
    public void loadsUserWithoutCountryOrCity() {
        ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        apiUser.setCountry(null);
        apiUser.setCity(null);
        testFixtures().insertUser(apiUser);

        User user = storage.loadUser(apiUser.getUrn()).blockingGet();

        assertThat(user).isEqualTo(getBaseUserBuilder(apiUser).city(absent()).country(absent()).build());
    }

    @Test
    public void loadsExtendedUser() {
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        testFixtures().insertExtendedUser(apiUser, DESCRIPTION, WEBSITE_URL, WEBSITE_NAME, DISCOGS_NAME, MYSPACE_NAME);

        User user = storage.loadUser(apiUser.getUrn()).blockingGet();

        assertThat(user).isEqualTo(getExtendedUserBuilder(apiUser).build());
    }

    @Test
    public void loadsFollowedUser() {
        ApiUser apiUser = testFixtures().insertUser();
        testFixtures().insertFollowing(apiUser.getUrn());

        User user = storage.loadUser(apiUser.getUrn()).blockingGet();

        assertThat(user).isEqualTo(
                getApiUserBuilder(apiUser).isFollowing(true).build());
    }

    @Test
    public void loadsUnfollowedUserPending() {
        ApiUser apiUser = testFixtures().insertUser();
        testFixtures().insertFollowingPendingRemoval(apiUser.getUrn(), 123);

        User user = storage.loadUser(apiUser.getUrn()).blockingGet();

        assertThat(user).isEqualTo(getApiUserBuilder(apiUser).build());
    }

    @Test
    public void loadUserWithArtistStation() {
        final Urn artistStation = Urn.forArtistStation(123);
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        testFixtures().insertUser(apiUser, artistStation);

        User user = storage.loadUser(apiUser.getUrn()).blockingGet();

        assertThat(user).isEqualTo(
                getApiUserBuilder(apiUser).artistStation(of(artistStation)).build());
    }

    @Test
    public void loadsUrnByPermalink() throws Exception {
        testFixtures().insertUser();
        ApiUser user = testFixtures().insertUser();
        String permalink = user.getPermalink();

        final Urn urn = storage.urnForPermalink(permalink).blockingGet();

        assertThat(urn).isEqualTo(user.getUrn());
    }

    @Test
    public void loadsUrnByPermalinkNotFound() throws Exception {
        testFixtures().insertUser();

        storage.urnForPermalink("testing")
               .test()
               .assertNoValues()
               .assertComplete();
    }

    private User.Builder getExtendedUserBuilder(ApiUser apiUser) {
        return getBaseUserBuilder(apiUser)
                .country(fromNullable(apiUser.getCountry()))
                .city(fromNullable(apiUser.getCity()))
                .description(of(DESCRIPTION))
                .websiteUrl(of(WEBSITE_URL))
                .websiteName(of(WEBSITE_NAME))
                .discogsName(of(DISCOGS_NAME))
                .mySpaceName(of(MYSPACE_NAME));
    }

    private User.Builder getApiUserBuilder(ApiUser apiUser) {
        return getBaseUserBuilder(apiUser)
                .country(fromNullable(apiUser.getCountry()))
                .city(fromNullable(apiUser.getCity()));
    }

    private User.Builder getBaseUserBuilder(ApiUser apiUser) {
        return ModelFixtures.userBuilder(false)
                            .urn(apiUser.getUrn())
                            .username(apiUser.getUsername())
                            .signupDate(apiUser.getCreatedAt())
                            .firstName(apiUser.getFirstName())
                            .lastName(apiUser.getLastName())
                            .followersCount(apiUser.getFollowersCount())
                            .followingsCount(apiUser.getFollowingsCount())
                            .avatarUrl(apiUser.getImageUrlTemplate())
                            .visualUrl(apiUser.getVisualUrlTemplate());
    }
}
