package com.soundcloud.android.onboarding.suggestions;

import static com.soundcloud.java.collections.Lists.newArrayList;

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
    private CategoryGroup categoryGroup;

    @Mock
    Category category1;
    @Mock
    Category category2;
    private SuggestedUser suggestedUser1;
    private SuggestedUser suggestedUser2;
    private SuggestedUser suggestedUser3;

    @Before
    public void before() {
        categoryGroup = new CategoryGroup(KEY);
        suggestedUser1 = new SuggestedUser("soundcloud:users:1");
        suggestedUser2 = new SuggestedUser("soundcloud:users:2");
        suggestedUser3 = new SuggestedUser("soundcloud:users:3");
    }

    @Test
    public void shouldImplementEqualsOnKey() {
        CategoryGroup categoryGroup = new CategoryGroup(KEY);
        categoryGroup.setCategories(newArrayList(new Category()));
        Expect.expect(this.categoryGroup).toEqual(categoryGroup);
    }

    @Test
    public void shouldReturnAllCategories() throws CreateModelException {
        final ArrayList<SuggestedUser> suggestedUsers = newArrayList(suggestedUser1, suggestedUser2);
        Mockito.when(category1.getUsers()).thenReturn(suggestedUsers);
        Mockito.when(category2.getUsers()).thenReturn(suggestedUsers);

        categoryGroup.setCategories(newArrayList(category1, category2));
        Expect.expect(categoryGroup.getNonEmptyCategories()).toContainExactly(category1, category2);
    }

    @Test
    public void shouldReturnAllSuggestedUsers() throws CreateModelException {
        Mockito.when(category1.getUsers()).thenReturn(ModelFixtures.create(SuggestedUser.class, 2));
        Mockito.when(category2.getUsers()).thenReturn(ModelFixtures.create(SuggestedUser.class, 2));
        categoryGroup.setCategories(newArrayList(category1, category2));
        Expect.expect(categoryGroup.getAllSuggestedUsers().size()).toBe(4);
    }

    @Test
    public void shouldReturnNonEmptyCategories() throws CreateModelException {
        final ArrayList<SuggestedUser> suggestedUsers = newArrayList(suggestedUser1, suggestedUser2);
        Mockito.when(category1.getUsers()).thenReturn(suggestedUsers);
        Mockito.when(category2.getUsers()).thenReturn(Collections.<SuggestedUser>emptyList());

        categoryGroup.setCategories(newArrayList(category1, category2));
        Expect.expect(categoryGroup.getNonEmptyCategories()).toContainExactly(category1);
    }

    @Test
    public void shouldBeEmptyWithEmptyCategories() throws CreateModelException {
        Mockito.when(category1.getUsers()).thenReturn(Collections.<SuggestedUser>emptyList());
        Mockito.when(category2.getUsers()).thenReturn(Collections.<SuggestedUser>emptyList());

        categoryGroup.setCategories(newArrayList(category1, category2));
        Expect.expect(categoryGroup.isEmpty()).toBeTrue();
    }

    @Test
    public void shouldNotBeEmptyWithProgressCategory() throws CreateModelException {
        Mockito.when(category1.getUsers()).thenReturn(Collections.<SuggestedUser>emptyList());
        categoryGroup.setCategories(newArrayList(category1, Category.progress()));
        Expect.expect(categoryGroup.isEmpty()).toBeFalse();
    }

    @Test
    public void shouldNotBeEmptyWithErrorCategory() throws CreateModelException {
        Mockito.when(category1.getUsers()).thenReturn(Collections.<SuggestedUser>emptyList());
        categoryGroup.setCategories(newArrayList(category1, Category.error()));
        Expect.expect(categoryGroup.isEmpty()).toBeFalse();
    }

    @Test
    public void shouldFilterDuplicatesFromCategories(){

        List<SuggestedUser> category1Users = newArrayList(suggestedUser1, suggestedUser2);
        List<SuggestedUser> category2Users = newArrayList(suggestedUser2, suggestedUser3);

        Mockito.when(category1.getUsers()).thenReturn(category1Users);
        Mockito.when(category2.getUsers()).thenReturn(category2Users);
        categoryGroup.setCategories(newArrayList(category1, category2));
        categoryGroup.removeDuplicateUsers(new HashSet<SuggestedUser>());
        Expect.expect(category2Users).toContainExactly(suggestedUser3);
    }

    @Test
    public void shouldNotReturnEmptyCateogoryAfterFiltering(){
        List<SuggestedUser> category1Users = newArrayList(suggestedUser1, suggestedUser2, suggestedUser3);
        List<SuggestedUser> category2Users = newArrayList(suggestedUser2, suggestedUser3);

        Mockito.when(category1.getUsers()).thenReturn(category1Users);
        Mockito.when(category2.getUsers()).thenReturn(category2Users);
        categoryGroup.setCategories(newArrayList(category1, category2));

        categoryGroup.removeDuplicateUsers(new HashSet<SuggestedUser>());
        Expect.expect(category2Users).toBeEmpty();
        Expect.expect(categoryGroup.getNonEmptyCategories()).not.toContain(category2);
    }

    @Test
    public void shouldFilterDuplicatesFromCategoriesOverMultipleGroups(){
        List<SuggestedUser> category1Users = newArrayList(suggestedUser1, suggestedUser2);
        List<SuggestedUser> category2Users = newArrayList(suggestedUser2, suggestedUser3);

        Mockito.when(category1.getUsers()).thenReturn(category1Users);
        Mockito.when(category2.getUsers()).thenReturn(category2Users);
        categoryGroup.setCategories(newArrayList(category1));

        CategoryGroup categoryGroup2 = new CategoryGroup("Key2");
        categoryGroup2.setCategories(newArrayList(category2));

        final HashSet<SuggestedUser> uniqueSuggestedUsersSet = new HashSet<>();
        categoryGroup.removeDuplicateUsers(uniqueSuggestedUsersSet);
        categoryGroup2.removeDuplicateUsers(uniqueSuggestedUsersSet);
        Expect.expect(category2Users).toContainExactly(suggestedUser3);

    }
}
