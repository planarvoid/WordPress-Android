package com.soundcloud.android.fragment;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.soundcloud.android.adapter.SuggestedUsersAdapter;
import com.soundcloud.android.model.Genre;
import com.soundcloud.android.model.GenreBucket;
import com.soundcloud.android.onboarding.OnboardingOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUsersFragmentTest {

    private SuggestedUsersFragment fragment;
    private SuggestedUsersAdapter adapter;

    @Before
    public void setup() {
        OnboardingOperations operations = mock(OnboardingOperations.class);
        when(operations.getGenreBuckets()).thenReturn(
                Observable.from(new GenreBucket(new Genre()), new GenreBucket(new Genre())).cache());
        adapter = new SuggestedUsersAdapter();
        fragment = new SuggestedUsersFragment(operations, adapter);
        Robolectric.shadowOf(fragment).setActivity(new SherlockFragmentActivity());
    }

    @Test
    public void shouldFetchGenreBucketsIntoListAdapterInOnCreate() {
        fragment.onCreate(null);
        expect(adapter.getCount()).toBe(2);
    }
}
