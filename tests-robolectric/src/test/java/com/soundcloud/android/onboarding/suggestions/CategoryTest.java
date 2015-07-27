package com.soundcloud.android.onboarding.suggestions;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.Expect;
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

    private Category category;
    private SuggestedUser[] allSuggestedUsers;

    @Before
    public void before() {
        category = new Category();
        allSuggestedUsers = new SuggestedUser[]{new SuggestedUser("soundcloud:users:1"),
                new SuggestedUser("soundcloud:users:2"),
                new SuggestedUser("soundcloud:users:3")};
        category.setUsers(newArrayList(allSuggestedUsers));
    }

    @Test
    public void shouldBeParcelable(){
        category.setKey("trapstep");
        category.setDisplayType(Category.DisplayType.PROGRESS);

        Parcel parcel = Parcel.obtain();
        category.writeToParcel(parcel, 0);

        Category category = new Category(parcel);
        Expect.expect(category.getKey()).toEqual(this.category.getKey());
        Expect.expect(category.getDisplayType()).toEqual(this.category.getDisplayType());
        /*
        Not implemented by Robolectric
        expect(category.getUsers()).toEqual(category.getUsers());
         */
    }

    @Test
    public void shouldReturnAllUsersAsFollowed() throws Exception {
        Set<Long> followedSet = Sets.newSet(1L, 2L, 3L);
        Expect.expect(category.getFollowedUsers(followedSet)).toContainExactly(allSuggestedUsers);
    }

    @Test
    public void shouldReturnPartialUsersAsFollowed() throws Exception {
        Set<Long> followedSet = Sets.newSet(2L, 3L);
        Expect.expect(category.getFollowedUsers(followedSet)).toContainExactly(Arrays.copyOfRange(allSuggestedUsers, 1, 3));
    }

    @Test
    public void shouldReturnNoUsersAsFollowed() throws Exception {
        Set<Long> followedSet = Sets.newSet();
        Expect.expect(category.getFollowedUsers(followedSet)).toBeEmpty();
    }

    @Test
    public void shouldReturnAllUsersAsNotFollowed() throws Exception {
        Set<Long> followedSet = Sets.newSet();
        Expect.expect(category.getNotFollowedUsers(followedSet)).toContainExactly(allSuggestedUsers);
    }

    @Test
    public void shouldReturnPartialUsersAsNotFollowed() throws Exception {
        Set<Long> followedSet = Sets.newSet(2L, 3L);
        Expect.expect(category.getNotFollowedUsers(followedSet)).toContainExactly(Arrays.copyOfRange(allSuggestedUsers, 0, 1));
    }

    @Test
    public void shouldReturnNoUsersAsNotFollowed() throws Exception {
        Set<Long> followedSet = Sets.newSet(1L, 2L, 3L);
        Expect.expect(category.getNotFollowedUsers(followedSet)).toBeEmpty();
    }

    @Test
    public void isFollowedShouldReturnFalseIfNoUserIsBeingFollowed() {
        Expect.expect(category.isFollowed(Collections.<Long>emptySet())).toBeFalse();
        Expect.expect(category.isFollowed(Sets.newSet(100L))).toBeFalse();
    }

    @Test
    public void isFollowedShouldReturnTrueIfAtLeastOneUserIsBeingFollowed() {
        Expect.expect(category.isFollowed(Sets.newSet(1L, 100L))).toBeTrue();
    }

    @Test
    public void shouldReturnEmptyMessage() {
        category.setDisplayType(Category.DisplayType.EMPTY);
        checkEmptyMessage(R.string.suggested_users_section_empty);
    }

    @Test
    public void shouldReturnErrorMessage() {
        category.setDisplayType(Category.DisplayType.ERROR);
        checkEmptyMessage(R.string.suggested_users_section_error);
    }

    @Test
    public void shouldReturnNullEmptyMessage() {
        category.setDisplayType(Category.DisplayType.DEFAULT);
        Expect.expect(category.getEmptyMessage(Robolectric.application.getResources())).toBeNull();
    }

    @Test
    public void shouldBeFacebookCategoryForFriends() {
        category.setKey("facebook_friends");
        Expect.expect(category.isFacebookCategory()).toBeTrue();
    }

    @Test
    public void shouldBeFacebookCategoryForLikes() {
        category.setKey("facebook_likes");
        Expect.expect(category.isFacebookCategory()).toBeTrue();
    }

    @Test
    public void shouldNotBeFacebookCategoryForOtherCategories() {
        category.setKey("smooth_jazz");
        Expect.expect(category.isFacebookCategory()).toBeFalse();
    }

    private void checkEmptyMessage(int expectedMessageResId) {
        final String emptyMessage = category.getEmptyMessage(Robolectric.application.getResources());
        Expect.expect(emptyMessage).toEqual(Robolectric.application.getResources().getString(expectedMessageResId));
    }

}
