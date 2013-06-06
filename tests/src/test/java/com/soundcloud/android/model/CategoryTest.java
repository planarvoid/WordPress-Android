package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.pivotallabs.greatexpectations.Expect;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

@RunWith(SoundCloudTestRunner.class)
public class CategoryTest {

    private Category mCategory;
    private SuggestedUser[] mAllSuggestedUsers;

    @Before
    public void before() {
        mCategory = new Category();
        mAllSuggestedUsers = new SuggestedUser[]{new SuggestedUser("soundcloud:users:1"),
                new SuggestedUser("soundcloud:users:2"),
                new SuggestedUser("soundcloud:users:3")};
        mCategory.setUsers(Lists.newArrayList(mAllSuggestedUsers));
    }

    @Test
    public void shouldReturnAllUsersAsFollowed() throws Exception {
        Set<Long> followedSet = Sets.newSet(1L, 2L, 3L);
        Expect.expect(mCategory.getFollowedUsers(followedSet)).toContainExactly(mAllSuggestedUsers);
    }

    @Test
    public void shouldReturnPartialUsersAsFollowed() throws Exception {
        Set<Long> followedSet = Sets.newSet(2L, 3L);
        Expect.expect(mCategory.getFollowedUsers(followedSet)).toContainExactly(Arrays.copyOfRange(mAllSuggestedUsers,1,3));
    }

    @Test
    public void shouldReturnNoUsersAsFollowed() throws Exception {
        Set<Long> followedSet = Sets.newSet();
        Expect.expect(mCategory.getFollowedUsers(followedSet)).toBeEmpty();
    }

    @Test
    public void isFollowedShouldReturnFalseIfNoUserIsBeingFollowed() {
        expect(mCategory.isFollowed(Collections.<Long>emptySet())).toBeFalse();
        expect(mCategory.isFollowed(Sets.newSet(100L))).toBeFalse();
    }

    @Test
    public void isFollowedShouldReturnTrueIfAtLeastOneUserIsBeingFollowed() {
        expect(mCategory.isFollowed(Sets.newSet(1L, 100L))).toBeTrue();
    }
}
