package com.soundcloud.android.fragment;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SuggestedUsersCategoriesAdapter;
import com.soundcloud.android.api.SuggestedUsersOperations;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.operations.following.FollowStatus;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUsersCategoriesFragmentTest {

    private SuggestedUsersCategoriesFragment fragment;
    private SuggestedUsersCategoriesAdapter adapter;
    @Mock
    private FollowStatus followStatus;
    @Mock
    private Observer<CategoryGroup> observer;
    @Mock
    private FollowingOperations followingOperations;

    @Before
    public void setup() throws CreateModelException {
        SuggestedUsersOperations operations = mock(SuggestedUsersOperations.class);
        when(operations.getCategoryGroups()).thenReturn(Observable.from(audio(), music()).cache());

        adapter = new SuggestedUsersCategoriesAdapter(SuggestedUsersCategoriesAdapter.Section.ALL_SECTIONS, followingOperations, followStatus);
        fragment = spy(new SuggestedUsersCategoriesFragment(operations, observer, adapter));

        SherlockFragmentActivity fragmentActivity = new SherlockFragmentActivity();
        when(fragment.getLayoutInflater(null)).thenReturn(fragmentActivity.getLayoutInflater());
        Robolectric.shadowOf(fragment).setActivity(fragmentActivity);
    }

    @Test
    public void shouldFetchGenreBucketsIntoListAdapterInOnCreate() {
        fragment.onViewCreated(View.inflate(Robolectric.application, R.layout.suggested_users_fragment, null), null);
        verify(observer, times(2)).onNext(any(CategoryGroup.class));
        verify(observer).onCompleted();
    }

    private CategoryGroup music() throws CreateModelException {
        return TestHelper.buildCategoryGroup(CategoryGroup.KEY_MUSIC, 3);
    }

    private CategoryGroup audio() throws CreateModelException {
        return TestHelper.buildCategoryGroup(CategoryGroup.KEY_SPEECH_AND_SOUNDS, 4);
    }

}
