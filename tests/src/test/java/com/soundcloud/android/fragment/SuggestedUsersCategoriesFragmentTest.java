package com.soundcloud.android.fragment;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.adapter.SuggestedUsersCategoriesAdapter;
import com.soundcloud.android.api.SuggestedUsersOperations;
import com.soundcloud.android.model.CategoryGroup;
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

import android.os.Bundle;
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
        when(operations.getCategoryGroups()).thenReturn(Observable.from(audio(), music()).cache());

        fragment = spy(new SuggestedUsersCategoriesFragment(operations, observer, adapter));

        SherlockFragmentActivity fragmentActivity = new SherlockFragmentActivity();
        when(fragment.getLayoutInflater(null)).thenReturn(fragmentActivity.getLayoutInflater());
        Robolectric.shadowOf(fragment).setActivity(fragmentActivity);
    }

    @Test
    public void shouldSetAdapterSectionsToExcludeFacebook() {
        final Bundle args = new Bundle();
        args.putString(SignupVia.EXTRA, SignupVia.API.getSignupIdentifier());
        fragment.setArguments(args);
        fragment.onCreate(null);
        verify(adapter).setActiveSections(SuggestedUsersCategoriesAdapter.Section.ALL_EXCEPT_FACEBOOK);
    }

    @Test
    public void shouldSetAdapterSectionsToIncludeFacebookWhenSignupViaFacebookSSO() {
        final Bundle args = new Bundle();
        args.putString(SignupVia.EXTRA, SignupVia.FACEBOOK_SSO.getSignupIdentifier());
        fragment.setArguments(args);
        fragment.onCreate(null);
        verify(adapter).setActiveSections(SuggestedUsersCategoriesAdapter.Section.ALL_SECTIONS);
    }

    @Test
    public void shouldSetAdapterSectionsToIncludeFacebookWhenSignupViaFacebookWebflow() {
        final Bundle args = new Bundle();
        args.putString(SignupVia.EXTRA, SignupVia.FACEBOOK_WEBFLOW.getSignupIdentifier());
        fragment.setArguments(args);
        fragment.onCreate(null);
        verify(adapter).setActiveSections(SuggestedUsersCategoriesAdapter.Section.ALL_SECTIONS);
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
