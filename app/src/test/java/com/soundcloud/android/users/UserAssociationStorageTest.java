package com.soundcloud.android.users;

import static com.soundcloud.android.storage.Tables.UserAssociations.ADDED_AT;
import static com.soundcloud.android.storage.Tables.UserAssociations.ASSOCIATION_TYPE;
import static com.soundcloud.android.storage.Tables.UserAssociations.REMOVED_AT;
import static com.soundcloud.android.storage.Tables.UserAssociations.TARGET_ID;
import static com.soundcloud.android.storage.Tables.UserAssociations.TYPE_FOLLOWING;
import static com.soundcloud.propeller.ContentValuesBuilder.values;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.Following;
import com.soundcloud.android.storage.Tables.UserAssociations;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.query.Query;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Date;
import java.util.List;

@SuppressWarnings("unchecked")
public class UserAssociationStorageTest extends StorageIntegrationTest {

    private UserAssociationStorage storage;

    private TestSubscriber<List<Following>> subscriber = new TestSubscriber<>();
    private TestSubscriber<List<Urn>> urnSubscriber = new TestSubscriber<>();

    private User followingAndFollower;
    private User following;
    private Urn followingUrn;
    private Urn followingAndFollowerUrn;

    @Before
    public void setUp() throws Exception {
        storage = new UserAssociationStorage(propeller());

        final ApiUser apiFollower = testFixtures().insertUser();
        final ApiUser apiFollowingAndFollower = testFixtures().insertUser();
        final ApiUser apiFollowing = testFixtures().insertUser();

        followingUrn = apiFollowing.getUrn();
        followingAndFollowerUrn = apiFollowingAndFollower.getUrn();

        testFixtures().insertFollower(apiFollower.getUrn(), 1);
        testFixtures().insertFollower(apiFollowingAndFollower.getUrn(), 2);
        testFixtures().insertFollowing(apiFollowingAndFollower.getUrn(), 2);
        testFixtures().insertFollowing(apiFollowing.getUrn(), 3);

        followingAndFollower = ModelFixtures.user(apiFollowingAndFollower).toBuilder().isFollowing(true).build();
        following = ModelFixtures.user(apiFollowing).toBuilder().isFollowing(true).build();
    }

    @Test
    public void shouldClearTable() {
        assertThat(select(Query.from(UserAssociations.TABLE))).counts(4);

        storage.clear();

        assertThat(select(Query.from(UserAssociations.TABLE))).isEmpty();
    }

    @Test
    public void shouldLoadIdsOfFollowingUsers() {
        Assertions.assertThat(storage.loadFollowedUserIds()).containsOnly(
                following.urn().getNumericId(),
                followingAndFollower.urn().getNumericId()
        );
    }

    @Test
    public void shouldLoadIdsOfFollowingUsersUnlessPendingAddition() {
        markFollowingAsAdded(followingAndFollower.urn());
        Assertions.assertThat(storage.loadFollowedUserIds()).containsOnly(
                following.urn().getNumericId()
        );
    }

    @Test
    public void shouldLoadIdsOfFollowingUsersUnlessPendingRemoval() {
        markFollowingAsRemoved(followingAndFollower.urn());
        Assertions.assertThat(storage.loadFollowedUserIds()).containsOnly(
                following.urn().getNumericId()
        );
    }

    @Test
    public void loadFollowingLoadsSingleFollowing() {
        final TestSubscriber<User> subscriber = new TestSubscriber<>();
        storage.followedUser(following.urn()).subscribe(subscriber);

        subscriber.assertValues(following);
    }

    @Test
    public void loadFollowingsLoadsAllFollowings() {
        storage.followedUsers(3, 0).subscribe(subscriber);

        subscriber.assertValues(
                asList(Following.from(followingAndFollower,
                                      UserAssociation.create(followingAndFollower.urn(), 2, 2, Optional.absent(), Optional.absent())),
                       Following.from(following,
                                      UserAssociation.create(following.urn(), 3, 2, Optional.absent(), Optional.absent())
                )
        ));
    }

    @Test
    public void loadFollowingsAdheresToLimit() {
        storage.followedUsers(1, 0).subscribe(subscriber);

        subscriber.assertValues(singletonList(
                Following.from(followingAndFollower,
                               UserAssociation.create(followingAndFollower.urn(), 2L, 2, Optional.absent(), Optional.absent()))));
    }

    @Test
    public void loadFollowingsAdheresToPosition() {
        storage.followedUsers(2, 2).subscribe(subscriber);

        subscriber.assertValues(singletonList(
                Following.from(following,
                               UserAssociation.create(following.urn(), 3L, 2, Optional.absent(), Optional.absent()))));
    }


    @Test
    public void loadFollowingsUrnsLoadsAllFollowings() {
        storage.followedUserUrns(3, 0).subscribe(urnSubscriber);

        urnSubscriber.assertValues(
                asList(
                        followingAndFollower.urn(),
                        following.urn()
                )
        );
    }

    @Test
    public void loadFollowingsUrnsAdheresToLimit() {
        storage.followedUserUrns(1, 0).subscribe(urnSubscriber);
        urnSubscriber.assertValues(singletonList(followingAndFollower.urn()));
    }

    @Test
    public void loadFollowingsUrnsAdheresToPosition() {
        storage.followedUserUrns(2, 2).subscribe(urnSubscriber);
        urnSubscriber.assertValues(singletonList(following.urn()));
    }

    @Test
    public void loadsStaleFollowingsWhenPendingAddition() {
        testFixtures().insertFollowingPendingAddition(followingUrn, 123);

        final List<Following> followings = storage.loadStaleFollowings();

        Assertions.assertThat(followings).containsExactly(
                Following.from(following,
                               UserAssociation.create(following.urn(), 0L, 2, Optional.of(new Date(123)), Optional.absent())));
    }

    @Test
    public void loadsStaleFollowingsWhenPendingRemoval() {
        testFixtures().insertFollowingPendingRemoval(followingUrn, 123);

        final List<Following> followings = storage.loadStaleFollowings();

        Assertions.assertThat(followings).containsExactly(
                Following.from(following.toBuilder().isFollowing(false).build(),
                               UserAssociation.create(following.urn(), 0L, 2, Optional.absent(), Optional.of(new Date(123)))));
    }

    @Test
    public void hasStaleFollowingsIsFalseWhenAddedAtOrRemovedAtNotSet() {
        Assertions.assertThat(storage.hasStaleFollowings()).isFalse();
    }

    @Test
    public void hasStaleFollowingsIsTrueIfAddedAtIsSet() {
        markFollowingAsAdded(followingUrn);

        Assertions.assertThat(storage.hasStaleFollowings()).isTrue();
    }

    @Test
    public void hasStaleFollowingsIsTrueIfRemovedAtIsSet() {
        markFollowingAsRemoved(followingUrn);

        Assertions.assertThat(storage.hasStaleFollowings()).isTrue();
    }

    @Test
    public void shouldDeleteFollowingsById() {
        storage.deleteFollowingsById(singletonList(followingAndFollowerUrn.getNumericId()));

        // should not delete the FOLLOWER association
        assertThat(select(Query.from(UserAssociations.TABLE)
                               .whereEq(TARGET_ID, followingAndFollowerUrn.getNumericId())))
                .counts(1);
        // should only delete the FOLLOWING association
        assertThat(select(Query.from(UserAssociations.TABLE)
                               .whereEq(TARGET_ID, followingAndFollowerUrn.getNumericId())
                               .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING)))
                .isEmpty();
    }

    @Test
    public void shouldResetPendingFollowingAddition() {
        final Query addedAtQuery = Query.from(UserAssociations.TABLE)
                                        .select(ADDED_AT)
                                        .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING)
                                        .whereEq(TARGET_ID, followingUrn.getNumericId());
        markFollowingAsAdded(followingUrn);
        long addedAtBefore = propeller().query(addedAtQuery).firstOrDefault(Long.class, 0L);
        Assertions.assertThat(addedAtBefore).isGreaterThan(0L);

        storage.updateFollowingFromPendingState(followingUrn);

        long addedAtAfter = propeller().query(addedAtQuery).firstOrDefault(Long.class, 0L);
        Assertions.assertThat(addedAtAfter).isEqualTo(0L);
    }

    @Test
    public void shouldDeletePendingFollowingRemoval() {
        markFollowingAsRemoved(followingUrn);
        assertThat(select(Query.from(UserAssociations.TABLE)
                               .whereEq(TARGET_ID, followingUrn.getNumericId())
                               .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING)))
                .counts(1);

        storage.updateFollowingFromPendingState(followingUrn);

        assertThat(select(Query.from(UserAssociations.TABLE)
                               .whereEq(TARGET_ID, followingUrn.getNumericId())
                               .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING)))
                .isEmpty();
    }

    @Test
    public void shouldNotUpdateFollowingFromPendingStateIfNotFound() {
        storage.updateFollowingFromPendingState(Urn.forUser(99999));
    }

    private void markFollowingAsAdded(Urn followingUrn) {
        propeller().update(UserAssociations.TABLE,
                           values().put(ADDED_AT, 123).get(),
                           filter().whereEq(TARGET_ID, followingUrn.getNumericId()));
    }

    private void markFollowingAsRemoved(Urn followingUrn) {
        propeller().update(UserAssociations.TABLE,
                           values().put(REMOVED_AT, 123).get(),
                           filter().whereEq(TARGET_ID, followingUrn.getNumericId()));
    }

}
