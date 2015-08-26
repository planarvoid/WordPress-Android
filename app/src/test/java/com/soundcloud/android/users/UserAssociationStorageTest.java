package com.soundcloud.android.users;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
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
    private TestSubscriber<List<Urn>> urnSubscriber = new TestSubscriber<>();

    private PropertySet user1;
    private PropertySet user2;
    private PropertySet user3;

    @Before
    public void setUp() throws Exception {
        storage = new UserAssociationStorage(propellerRx());

        final ApiUser apiUser1 = testFixtures().insertUser();
        final ApiUser apiUser2 = testFixtures().insertUser();
        final ApiUser apiUser3 = testFixtures().insertUser();

        testFixtures().insertFollower(apiUser1.getUrn(), 1);
        testFixtures().insertFollower(apiUser2.getUrn(), 2);
        testFixtures().insertFollowing(apiUser2.getUrn(), 2);
        testFixtures().insertFollowing(apiUser3.getUrn(), 3);

        user1 = apiUserToResultSet(apiUser1);
        user2 = apiUserToResultSet(apiUser2);
        user3 = apiUserToResultSet(apiUser3);
    }

    @Test
    public void loadFollowersLoadsAllFollowers() {
        storage.loadFollowers(3, 0).subscribe(subscriber);

        subscriber.assertValues(
                Arrays.asList(
                        user1.put(UserProperty.IS_FOLLOWED_BY_ME, false)
                                .put(UserAssociationProperty.POSITION, 1L),
                        user2.put(UserProperty.IS_FOLLOWED_BY_ME, true)
                                .put(UserAssociationProperty.POSITION, 2L)
                )
        );
    }

    @Test
    public void loadFollowersAdheresToLimit() {
        storage.loadFollowers(1, 0).subscribe(subscriber);

        subscriber.assertValues(Collections.singletonList(
                user1.put(UserProperty.IS_FOLLOWED_BY_ME, false)
                        .put(UserAssociationProperty.POSITION, 1L)));
    }

    @Test
    public void loadFollowersAdheresToPosition() {
        storage.loadFollowers(2, 1).subscribe(subscriber);

        subscriber.assertValues(Collections.singletonList(
                user2.put(UserProperty.IS_FOLLOWED_BY_ME, true)
                        .put(UserAssociationProperty.POSITION, 2L)));
    }

    @Test
    public void loadFollowingsLoadsAllFollowings() {
        storage.loadFollowings(3, 0).subscribe(subscriber);

        subscriber.assertValues(
                Arrays.asList(
                        user2.put(UserProperty.IS_FOLLOWED_BY_ME, true)
                                .put(UserAssociationProperty.POSITION, 2L),
                        user3.put(UserProperty.IS_FOLLOWED_BY_ME, true)
                                .put(UserAssociationProperty.POSITION, 3L)
                )
        );
    }

    @Test
    public void loadFollowingsAdheresToLimit() {
        storage.loadFollowings(1, 0).subscribe(subscriber);

        subscriber.assertValues(Collections.singletonList(
                user2.put(UserProperty.IS_FOLLOWED_BY_ME, true)
                        .put(UserAssociationProperty.POSITION, 2L)));
    }

    @Test
    public void loadFollowingsAdheresToPosition() {
        storage.loadFollowings(2, 2).subscribe(subscriber);

        subscriber.assertValues(Collections.singletonList(
                user3.put(UserProperty.IS_FOLLOWED_BY_ME, true)
                        .put(UserAssociationProperty.POSITION, 3L)));
    }


    @Test
    public void loadFollowingsUrnsLoadsAllFollowings() {
        storage.loadFollowingsUrns(3, 0).subscribe(urnSubscriber);

        urnSubscriber.assertValues(
                Arrays.asList(
                        user2.get(UserProperty.URN),
                        user3.get(UserProperty.URN)
                )
        );
    }

    @Test
    public void loadFollowingsUrnsAdheresToLimit() {
        storage.loadFollowingsUrns(1, 0).subscribe(urnSubscriber);
        urnSubscriber.assertValues(Collections.singletonList(user2.get(UserProperty.URN)));
    }

    @Test
    public void loadFollowingsUrnsAdheresToPosition() {
        storage.loadFollowingsUrns(2, 2).subscribe(urnSubscriber);
        urnSubscriber.assertValues(Collections.singletonList(user3.get(UserProperty.URN)));
    }

    private PropertySet apiUserToResultSet(ApiUser apiUser1) {
        return apiUser1.toPropertySet().slice(UserProperty.URN,
                UserProperty.USERNAME, UserProperty.COUNTRY, UserProperty.FOLLOWERS_COUNT);
    }
}