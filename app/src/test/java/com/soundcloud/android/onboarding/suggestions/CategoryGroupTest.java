package com.soundcloud.android.onboarding.suggestions;

import com.google.common.collect.Lists;
import com.soundcloud.android.Expect;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class CategoryGroupTest {

    public static final String KEY = "key1";
    private CategoryGroup mCategoryGroup;

    @Mock
    Category category1;
    @Mock
    Category category2;
    private SuggestedUser suggestedUser1;
    private SuggestedUser suggestedUser2;
    private SuggestedUser suggestedUser3;

    @Before
    public void before() {
        mCategoryGroup = new CategoryGroup(KEY);
        suggestedUser1 = new SuggestedUser("soundcloud:users:1");
        suggestedUser2 = new SuggestedUser("soundcloud:users:2");
        suggestedUser3 = new SuggestedUser("soundcloud:users:3");
    }

    @Test
    public void shouldImplementEqualsOnKey() {
        CategoryGroup categoryGroup = new CategoryGroup(KEY);
        categoryGroup.setCategories(Lists.newArrayList(new Category()));
        Expect.expect(mCategoryGroup).toEqual(categoryGroup);
    }

    @Test
    public void shouldReturnAllCategories() throws CreateModelException {
        final ArrayList<SuggestedUser> suggestedUsers = Lists.newArrayList(suggestedUser1, suggestedUser2);
        Mockito.when(category1.getUsers()).thenReturn(suggestedUsers);
        Mockito.when(category2.getUsers()).thenReturn(suggestedUsers);

        mCategoryGroup.setCategories(Lists.newArrayList(category1, category2));
        Expect.expect(mCategoryGroup.getNonEmptyCategories()).toContainExactly(category1, category2);
    }

    @Test
    public void shouldReturnAllSuggestedUsers() throws CreateModelException {
        Mockito.when(category1.getUsers()).thenReturn(ModelFixtures.create(SuggestedUser.class, 2));
        Mockito.when(category2.getUsers()).thenReturn(ModelFixtures.create(SuggestedUser.class, 2));
        mCategoryGroup.setCategories(Lists.newArrayList(category1, category2));
        Expect.expect(mCategoryGroup.getAllSuggestedUsers().size()).toBe(4);
    }

    @Test
    public void shouldReturnNonEmptyCategories() throws CreateModelException {
        final ArrayList<SuggestedUser> suggestedUsers = Lists.newArrayList(suggestedUser1, suggestedUser2);
        Mockito.when(category1.getUsers()).thenReturn(suggestedUsers);
        Mockito.when(category2.getUsers()).thenReturn(Collections.<SuggestedUser>emptyList());

        mCategoryGroup.setCategories(Lists.newArrayList(category1, category2));
        Expect.expect(mCategoryGroup.getNonEmptyCategories()).toContainExactly(category1);
    }

    @Test
    public void shouldBeEmptyWithEmptyCategories() throws CreateModelException {
        Mockito.when(category1.getUsers()).thenReturn(Collections.<SuggestedUser>emptyList());
        Mockito.when(category2.getUsers()).thenReturn(Collections.<SuggestedUser>emptyList());

        mCategoryGroup.setCategories(Lists.newArrayList(category1, category2));
        Expect.expect(mCategoryGroup.isEmpty()).toBeTrue();
    }

    @Test
    public void shouldNotBeEmptyWithProgressCategory() throws CreateModelException {
        Mockito.when(category1.getUsers()).thenReturn(Collections.<SuggestedUser>emptyList());
        mCategoryGroup.setCategories(Lists.newArrayList(category1, Category.progress()));
        Expect.expect(mCategoryGroup.isEmpty()).toBeFalse();
    }

    @Test
    public void shouldNotBeEmptyWithErrorCategory() throws CreateModelException {
        Mockito.when(category1.getUsers()).thenReturn(Collections.<SuggestedUser>emptyList());
        mCategoryGroup.setCategories(Lists.newArrayList(category1, Category.error()));
        Expect.expect(mCategoryGroup.isEmpty()).toBeFalse();
    }

    @Test
    public void shouldFilterDuplicatesFromCategories(){

        List<SuggestedUser> category1Users = Lists.newArrayList(suggestedUser1, suggestedUser2);
        List<SuggestedUser> category2Users = Lists.newArrayList(suggestedUser2, suggestedUser3);

        Mockito.when(category1.getUsers()).thenReturn(category1Users);
        Mockito.when(category2.getUsers()).thenReturn(category2Users);
        mCategoryGroup.setCategories(Lists.newArrayList(category1, category2));
        mCategoryGroup.removeDuplicateUsers(new HashSet<SuggestedUser>());
        Expect.expect(category2Users).toContainExactly(suggestedUser3);
    }

    @Test
    public void shouldNotReturnEmptyCateogoryAfterFiltering(){
        List<SuggestedUser> category1Users = Lists.newArrayList(suggestedUser1, suggestedUser2, suggestedUser3);
        List<SuggestedUser> category2Users = Lists.newArrayList(suggestedUser2, suggestedUser3);

        Mockito.when(category1.getUsers()).thenReturn(category1Users);
        Mockito.when(category2.getUsers()).thenReturn(category2Users);
        mCategoryGroup.setCategories(Lists.newArrayList(category1, category2));

        mCategoryGroup.removeDuplicateUsers(new HashSet<SuggestedUser>());
        Expect.expect(category2Users).toBeEmpty();
        Expect.expect(mCategoryGroup.getNonEmptyCategories()).not.toContain(category2);
    }

    @Test
    public void shouldFilterDuplicatesFromCategoriesOverMultipleGroups(){
        List<SuggestedUser> category1Users = Lists.newArrayList(suggestedUser1, suggestedUser2);
        List<SuggestedUser> category2Users = Lists.newArrayList(suggestedUser2, suggestedUser3);

        Mockito.when(category1.getUsers()).thenReturn(category1Users);
        Mockito.when(category2.getUsers()).thenReturn(category2Users);
        mCategoryGroup.setCategories(Lists.newArrayList(category1));

        CategoryGroup categoryGroup2 = new CategoryGroup("Key2");
        categoryGroup2.setCategories(Lists.newArrayList(category2));

        final HashSet<SuggestedUser> uniqueSuggestedUsersSet = new HashSet<SuggestedUser>();
        mCategoryGroup.removeDuplicateUsers(uniqueSuggestedUsersSet);
        categoryGroup2.removeDuplicateUsers(uniqueSuggestedUsersSet);
        Expect.expect(category2Users).toContainExactly(suggestedUser3);

    }
}
