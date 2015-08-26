package com.soundcloud.android.users;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unchecked")
public class UserAssociationStorageTest extends StorageIntegrationTest {

    private UserAssociationStorage storage;

    private TestSubscriber<List<PropertySet>> subscriber = new TestSubscriber<>();
    private TestSubscriber<List<Urn>> urnSubscriber = new TestSubscriber<>();

    private PropertySet follower;
    private PropertySet followingAndFollower;
    private PropertySet following;

    @Before
    public void setUp() throws Exception {
        storage = new UserAssociationStorage(propellerRx());

        final ApiUser apiFollower = testFixtures().insertUser();
        final ApiUser apiFollowingAndFollower = testFixtures().insertUser();
        final ApiUser apiFollowing = testFixtures().insertUser();

        testFixtures().insertFollower(apiFollower.getUrn(), 1);
        testFixtures().insertFollower(apiFollowingAndFollower.getUrn(), 2);
        testFixtures().insertFollowing(apiFollowingAndFollower.getUrn(), 2);
        testFixtures().insertFollowing(apiFollowing.getUrn(), 3);

        follower = apiUserToResultSet(apiFollower);
        followingAndFollower = apiUserToResultSet(apiFollowingAndFollower);
        following = apiUserToResultSet(apiFollowing);
    }

    @Test
    public void loadFollowersLoadsAllFollowers() {
        storage.loadFollowers(3, 0).subscribe(subscriber);

        subscriber.assertValues(
                Arrays.asList(
                        follower.put(UserProperty.IS_FOLLOWED_BY_ME, false)
                                .put(UserAssociationProperty.POSITION, 1L),
                        followingAndFollower.put(UserProperty.IS_FOLLOWED_BY_ME, true)
                                .put(UserAssociationProperty.POSITION, 2L)
                )
        );
    }

    @Test
    public void loadFollowersAdheresToLimit() {
        storage.loadFollowers(1, 0).subscribe(subscriber);

        subscriber.assertValues(Collections.singletonList(
                follower.put(UserProperty.IS_FOLLOWED_BY_ME, false)
                        .put(UserAssociationProperty.POSITION, 1L)));
    }

    @Test
    public void loadFollowersAdheresToPosition() {
        storage.loadFollowers(2, 1).subscribe(subscriber);

        subscriber.assertValues(Collections.singletonList(
                followingAndFollower.put(UserProperty.IS_FOLLOWED_BY_ME, true)
                        .put(UserAssociationProperty.POSITION, 2L)));
    }

    @Test
    public void loadFollowingLoadsSingleFollowing() {
        final TestSubscriber<PropertySet> subscriber = new TestSubscriber<>();
        storage.loadFollowing(following.get(EntityProperty.URN)).subscribe(subscriber);

        subscriber.assertValues(
                following.put(UserProperty.IS_FOLLOWED_BY_ME, true)
                        .put(UserAssociationProperty.POSITION, 3L)
        );
    }

    @Test
    public void loadFollowingsLoadsAllFollowings() {
        storage.loadFollowings(3, 0).subscribe(subscriber);

        subscriber.assertValues(
                Arrays.asList(
                        followingAndFollower.put(UserProperty.IS_FOLLOWED_BY_ME, true)
                                .put(UserAssociationProperty.POSITION, 2L),
                        following.put(UserProperty.IS_FOLLOWED_BY_ME, true)
                                .put(UserAssociationProperty.POSITION, 3L)
                )
        );
    }

    @Test
    public void loadFollowingsAdheresToLimit() {
        storage.loadFollowings(1, 0).subscribe(subscriber);

        subscriber.assertValues(Collections.singletonList(
                followingAndFollower.put(UserProperty.IS_FOLLOWED_BY_ME, true)
                        .put(UserAssociationProperty.POSITION, 2L)));
    }

    @Test
    public void loadFollowingsAdheresToPosition() {
        storage.loadFollowings(2, 2).subscribe(subscriber);

        subscriber.assertValues(Collections.singletonList(
                following.put(UserProperty.IS_FOLLOWED_BY_ME, true)
                        .put(UserAssociationProperty.POSITION, 3L)));
    }


    @Test
    public void loadFollowingsUrnsLoadsAllFollowings() {
        storage.loadFollowingsUrns(3, 0).subscribe(urnSubscriber);

        urnSubscriber.assertValues(
                Arrays.asList(
                        followingAndFollower.get(UserProperty.URN),
                        following.get(UserProperty.URN)
                )
        );
    }

    @Test
    public void loadFollowingsUrnsAdheresToLimit() {
        storage.loadFollowingsUrns(1, 0).subscribe(urnSubscriber);
        urnSubscriber.assertValues(Collections.singletonList(followingAndFollower.get(UserProperty.URN)));
    }

    @Test
    public void loadFollowingsUrnsAdheresToPosition() {
        storage.loadFollowingsUrns(2, 2).subscribe(urnSubscriber);
        urnSubscriber.assertValues(Collections.singletonList(following.get(UserProperty.URN)));
    }

    private PropertySet apiUserToResultSet(ApiUser apiUser1) {
        return apiUser1.toPropertySet().slice(UserProperty.URN,
                UserProperty.USERNAME, UserProperty.COUNTRY, UserProperty.FOLLOWERS_COUNT);
    }
}
