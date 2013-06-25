package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
public class CategoryGroupTest {

    public static final String KEY = "key1";
    private CategoryGroup mCategoryGroup;

    @Mock
    Category category1;
    @Mock
    Category category2;

    @Before
    public void before() {
        mCategoryGroup = new CategoryGroup(KEY);
    }

    @Test
    public void shouldImplementEqualsOnKey() {
        CategoryGroup categoryGroup = new CategoryGroup(KEY);
        categoryGroup.setCategories(Lists.newArrayList(new Category("urn1")));
        expect(mCategoryGroup).toEqual(categoryGroup);
    }

    @Test
    public void shouldReturnAllCategories() throws CreateModelException {
        final ArrayList<SuggestedUser> suggestedUsers = Lists.newArrayList(new SuggestedUser("urn1"), new SuggestedUser("urn2"));
        when(category1.getUsers()).thenReturn(suggestedUsers);
        when(category2.getUsers()).thenReturn(suggestedUsers);

        mCategoryGroup.setCategories(Lists.newArrayList(category1, category2));
        expect(mCategoryGroup.getNonEmptyCategories()).toContainExactly(category1, category2);
    }

    @Test
    public void shouldReturnNonEmptyCategories() throws CreateModelException {
        final ArrayList<SuggestedUser> suggestedUsers = Lists.newArrayList(new SuggestedUser("urn1"), new SuggestedUser("urn2"));
        when(category1.getUsers()).thenReturn(suggestedUsers);
        when(category2.getUsers()).thenReturn(Collections.<SuggestedUser>emptyList());

        mCategoryGroup.setCategories(Lists.newArrayList(category1, category2));
        expect(mCategoryGroup.getNonEmptyCategories()).toContainExactly(category1);
    }

    @Test
    @Ignore
    public void shouldBeEmptyWithEmptyCategory() throws CreateModelException {
        when(category1.getUsers()).thenReturn(Collections.<SuggestedUser>emptyList());
        when(category2.getUsers()).thenReturn(Collections.<SuggestedUser>emptyList());

        mCategoryGroup.setCategories(Lists.newArrayList(category1, category2));
        expect(mCategoryGroup.isEmpty()).toBeTrue();
    }
}
