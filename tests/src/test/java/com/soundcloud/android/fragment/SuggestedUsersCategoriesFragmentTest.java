package com.soundcloud.android.fragment;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.soundcloud.android.adapter.SuggestedUsersCategoriesAdapter;
import com.soundcloud.android.operations.following.FollowStatus;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.onboarding.OnboardingOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.view.View;
import android.widget.ListView;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUsersCategoriesFragmentTest {

    private SuggestedUsersCategoriesFragment fragment;
    private SuggestedUsersCategoriesAdapter adapter;
    @Mock
    private FollowStatus followStatus;

    @Before
    public void setup() throws CreateModelException {
        OnboardingOperations operations = mock(OnboardingOperations.class);
        when(operations.getCategoryGroups()).thenReturn(Observable.from(audio(), music()).cache());

        adapter = new SuggestedUsersCategoriesAdapter(SuggestedUsersCategoriesAdapter.Section.ALL_SECTIONS, followStatus);
        fragment = spy(new SuggestedUsersCategoriesFragment(operations, adapter));

        SherlockFragmentActivity fragmentActivity = new SherlockFragmentActivity();
        when(fragment.getLayoutInflater(null)).thenReturn(fragmentActivity.getLayoutInflater());
        Robolectric.shadowOf(fragment).setActivity(fragmentActivity);
    }

    @Test
    public void shouldFetchGenreBucketsIntoListAdapterInOnCreate() {
        when(fragment.getListView()).thenReturn(new ListView(Robolectric.application));
        fragment.onViewCreated(new View(Robolectric.application), null);
        expect(adapter.getCount()).toBe(8);
    }

    private CategoryGroup music() throws CreateModelException {
        return TestHelper.buildCategoryGroup(CategoryGroup.KEY_MUSIC, 3);
    }

    private CategoryGroup audio() throws CreateModelException {
        return TestHelper.buildCategoryGroup(CategoryGroup.KEY_SPEECH_AND_SOUNDS, 4);
    }

}
