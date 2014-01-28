package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
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
        mCategory = new Category();
        mAllSuggestedUsers = new SuggestedUser[]{new SuggestedUser("soundcloud:users:1"),
                new SuggestedUser("soundcloud:users:2"),
                new SuggestedUser("soundcloud:users:3")};
        mCategory.setUsers(Lists.newArrayList(mAllSuggestedUsers));
    }

    @Test
    public void shouldBeParcelable(){
        mCategory.setKey("trapstep");
        mCategory.setDisplayType(Category.DisplayType.PROGRESS);

        Parcel parcel = Parcel.obtain();
        mCategory.writeToParcel(parcel, 0);

        Category category = new Category(parcel);
        expect(category.getKey()).toEqual(mCategory.getKey());
        expect(category.getDisplayType()).toEqual(mCategory.getDisplayType());
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

    @Test
    public void shouldReturnEmptyMessage() {
        mCategory.setDisplayType(Category.DisplayType.EMPTY);
        checkEmptyMessage(R.string.suggested_users_section_empty);
    }

    @Test
    public void shouldReturnErrorMessage() {
        mCategory.setDisplayType(Category.DisplayType.ERROR);
        checkEmptyMessage(R.string.suggested_users_section_error);
    }

    @Test
    public void shouldReturnNullEmptyMessage() {
        mCategory.setDisplayType(Category.DisplayType.DEFAULT);
        expect(mCategory.getEmptyMessage(Robolectric.application.getResources())).toBeNull();
    }

    @Test
    public void shouldBeFacebookCategoryForFriends() {
        mCategory.setKey("facebook_friends");
        expect(mCategory.isFacebookCategory()).toBeTrue();
    }

    @Test
    public void shouldBeFacebookCategoryForLikes() {
        mCategory.setKey("facebook_likes");
        expect(mCategory.isFacebookCategory()).toBeTrue();
    }

    @Test
    public void shouldNotBeFacebookCategoryForOtherCategories() {
        mCategory.setKey("smooth_jazz");
        expect(mCategory.isFacebookCategory()).toBeFalse();
    }

    private void checkEmptyMessage(int expectedMessageResId) {
        final String emptyMessage = mCategory.getEmptyMessage(Robolectric.application.getResources());
        expect(emptyMessage).toEqual(Robolectric.application.getResources().getString(expectedMessageResId));
    }

}
