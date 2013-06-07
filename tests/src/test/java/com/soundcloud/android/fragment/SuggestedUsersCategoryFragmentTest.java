package com.soundcloud.android.fragment;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Category;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Bundle;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUsersCategoryFragmentTest {

    private SuggestedUsersCategoryFragment fragment;

    @Before
    public void setup() throws CreateModelException {
        fragment = new SuggestedUsersCategoryFragment();
    }

    @Test
    public void shouldNotSetAdapterWithNoCategoryArguments() {
        fragment.onCreate(null);
        expect(fragment.getAdapter()).toBeNull();
    }

    @Test
    public void shouldCreateAdapterWhenCategoryInArgs() throws CreateModelException {
        Category category = TestHelper. getModelFactory().createModel(Category.class);

        Bundle args = new Bundle();
        args.putParcelable(SuggestedUsersCategoryFragment.KEY_CATEGORY, category);

        fragment.setArguments(args);
        fragment.onCreate(null);
        expect(fragment.getAdapter().getItem(0)).toBe(category.getUsers().get(0));
    }

    @Test
    public void shouldCreateAdapterAfterSetCategoryCalled() throws CreateModelException {
        fragment.onCreate(null);

        Category category = TestHelper. getModelFactory().createModel(Category.class);
        fragment.setCategory(category);

        expect(fragment.getAdapter().getItem(0)).toBe(category.getUsers().get(0));
    }

    @Test
    public void shouldSetArgsAfterSetCategoryCalled() throws CreateModelException {
        fragment.onCreate(null);

        Category category = TestHelper. getModelFactory().createModel(Category.class);
        fragment.setCategory(category);

        expect(fragment.getArguments().getParcelable(SuggestedUsersCategoryFragment.KEY_CATEGORY)).toBe(category);
    }
}
