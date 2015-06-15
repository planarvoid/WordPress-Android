package com.soundcloud.android.onboarding.suggestions;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUsersCategoriesFragmentTest {

    private SuggestedUsersCategoriesFragment fragment;
    @Mock
    private SuggestedUsersCategoriesAdapter adapter;
    @Mock
    private Observer<CategoryGroup> observer;

    @Before
    public void setup() throws CreateModelException {
        SuggestedUsersOperations operations = mock(SuggestedUsersOperations.class);
        when(operations.getCategoryGroups()).thenReturn(Observable.just(audio(), music()).cache());
        when(operations.getMusicAndSoundsSuggestions()).thenReturn(Observable.just(audio(), music()).cache());

        fragment = new SuggestedUsersCategoriesFragment(operations, observer, adapter);

        shadowOf(fragment).setActivity(new FragmentActivity());
    }

    @Test
    public void shouldSetAdapterSectionsToExcludeFacebook() {
        fragment.setArguments(new Bundle());
        fragment.onCreate(null);
        verify(adapter).setActiveSections(SuggestedUsersCategoriesAdapter.Section.ALL_EXCEPT_FACEBOOK);
    }

    @Test
    public void shouldSetAdapterSectionsToIncludeFacebook() {
        final Bundle args = new Bundle();
        args.putBoolean(SuggestedUsersCategoriesFragment.SHOW_FACEBOOK, true);
        fragment.setArguments(args);
        fragment.onCreate(null);
        verify(adapter).setActiveSections(SuggestedUsersCategoriesAdapter.Section.ALL_SECTIONS);
    }

    @Ignore // RL1 doesn't support dealing with resources from AARs
    @Test
    public void shouldFetchGenreBucketsIntoListAdapterInOnCreate() {
        fragment.onCreate(null);
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
