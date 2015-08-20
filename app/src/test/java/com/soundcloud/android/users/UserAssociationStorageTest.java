package com.soundcloud.android.users;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@SuppressWarnings("unchecked")
public class UserAssociationStorageTest extends StorageIntegrationTest {

    private static final Date FOLLOW_DATE_1 = new Date(10000);
    private static final Date FOLLOW_DATE_2 = new Date(20000);
    private static final Date FOLLOW_DATE_3 = new Date(30000);

    private UserAssociationStorage storage;

    private TestSubscriber<List<PropertySet>> subscriber = new TestSubscriber<>();
    private PropertySet user1;
    private PropertySet user2;
    private PropertySet user3;

    @Before
    public void setUp() throws Exception {
        storage = new UserAssociationStorage(propellerRx());

        final ApiUser apiUser1 = testFixtures().insertUser();
        final ApiUser apiUser2 = testFixtures().insertUser();
        final ApiUser apiUser3 = testFixtures().insertUser();

        testFixtures().insertFollower(apiUser1.getUrn(), FOLLOW_DATE_1.getTime());
        testFixtures().insertFollower(apiUser2.getUrn(), FOLLOW_DATE_2.getTime());
        testFixtures().insertFollowing(apiUser2.getUrn(), FOLLOW_DATE_2.getTime());
        testFixtures().insertFollowing(apiUser3.getUrn(), FOLLOW_DATE_3.getTime());

        user1 = apiUserToResultSet(apiUser1);
        user2 = apiUserToResultSet(apiUser2);
        user3 = apiUserToResultSet(apiUser3);
    }

    @Test
    public void loadFollowersLoadsAllFollowers() {
        storage.loadFollowers(3, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValues(
                Arrays.asList(
                        user2.put(UserProperty.IS_FOLLOWED_BY_ME, true),
                        user1.put(UserProperty.IS_FOLLOWED_BY_ME, false)
                )
        );
    }

    @Test
    public void loadFollowersAdheresToLimit() {
        storage.loadFollowers(1, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValues(Collections.singletonList(user2.put(UserProperty.IS_FOLLOWED_BY_ME, true)));
    }

    @Test
    public void loadFollowersAdheresToTimestamp() {
        storage.loadFollowers(2, FOLLOW_DATE_2.getTime()).subscribe(subscriber);

        subscriber.assertValues(Collections.singletonList(user1.put(UserProperty.IS_FOLLOWED_BY_ME, false)));
    }

    @Test
    public void loadFollowingsLoadsAllFollowings() {
        storage.loadFollowings(3, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValues(
                Arrays.asList(
                        user3.put(UserProperty.IS_FOLLOWED_BY_ME, true),
                        user2.put(UserProperty.IS_FOLLOWED_BY_ME, true)
                )
        );
    }

    @Test
    public void loadFollowingsAdheresToLimit() {
        storage.loadFollowings(1, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValues(Collections.singletonList(user3.put(UserProperty.IS_FOLLOWED_BY_ME, true)));
    }

    @Test
    public void loadFollowingsAdheresToTimestamp() {
        storage.loadFollowings(2, FOLLOW_DATE_3.getTime()).subscribe(subscriber);

        subscriber.assertValues(Collections.singletonList(user2.put(UserProperty.IS_FOLLOWED_BY_ME, true)));
    }

    private PropertySet apiUserToResultSet(ApiUser apiUser1) {
        return apiUser1.toPropertySet().slice(UserProperty.URN,
                UserProperty.USERNAME, UserProperty.COUNTRY, UserProperty.FOLLOWERS_COUNT);
    }
}