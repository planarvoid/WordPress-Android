package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;

import android.os.Parcel;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

@RunWith(SoundCloudTestRunner.class)
public class CategoryTest {

    private Category mCategory;
    private SuggestedUser[] mAllSuggestedUsers;

    @Before
    public void before() {
        mCategory = new Category("soundcloud:categories:123");
        mAllSuggestedUsers = new SuggestedUser[]{new SuggestedUser("soundcloud:users:1"),
                new SuggestedUser("soundcloud:users:2"),
                new SuggestedUser("soundcloud:users:3")};
        mCategory.setUsers(Lists.newArrayList(mAllSuggestedUsers));
    }

    @Test
    public void shouldBeParcelable(){
        mCategory.setName("TrapStep");
        mCategory.setPermalink("trapstep");
        mCategory.setType(Category.Type.PROGRESS);

        Parcel parcel = Parcel.obtain();
        mCategory.writeToParcel(parcel, 0);

        Category category = new Category(parcel);
        expect(category.getName()).toEqual(mCategory.getName());
        expect(category.getPermalink()).toEqual(mCategory.getPermalink());
        expect(category.getType()).toEqual(mCategory.getType());
        /*
        Not implemented by Robolectric
        expect(category.getUsers()).toEqual(mCategory.getUsers());
         */
    }

    @Test
    public void shouldReturnAllUsersAsFollowed() throws Exception {
        Set<Long> followedSet = Sets.newSet(1L, 2L, 3L);
        expect(mCategory.getFollowedUsers(followedSet)).toContainExactly(mAllSuggestedUsers);
    }

    @Test
    public void shouldReturnPartialUsersAsFollowed() throws Exception {
        Set<Long> followedSet = Sets.newSet(2L, 3L);
        expect(mCategory.getFollowedUsers(followedSet)).toContainExactly(Arrays.copyOfRange(mAllSuggestedUsers,1,3));
    }

    @Test
    public void shouldReturnNoUsersAsFollowed() throws Exception {
        Set<Long> followedSet = Sets.newSet();
        expect(mCategory.getFollowedUsers(followedSet)).toBeEmpty();
    }

    @Test
    public void shouldReturnAllUsersAsNotFollowed() throws Exception {
        Set<Long> followedSet = Sets.newSet();
        expect(mCategory.getNotFollowedUsers(followedSet)).toContainExactly(mAllSuggestedUsers);
    }

    @Test
    public void shouldReturnPartialUsersAsNotFollowed() throws Exception {
        Set<Long> followedSet = Sets.newSet(2L, 3L);
        expect(mCategory.getNotFollowedUsers(followedSet)).toContainExactly(Arrays.copyOfRange(mAllSuggestedUsers,0,1));
    }

    @Test
    public void shouldReturnNoUsersAsNotFollowed() throws Exception {
        Set<Long> followedSet = Sets.newSet(1L, 2L, 3L);
        expect(mCategory.getNotFollowedUsers(followedSet)).toBeEmpty();
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
