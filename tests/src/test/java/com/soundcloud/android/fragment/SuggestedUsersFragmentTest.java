package com.soundcloud.android.fragment;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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

import android.view.View;
import android.widget.ListView;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUsersFragmentTest {

    private SuggestedUsersFragment fragment;
    private SuggestedUsersAdapter adapter;

    @Before
    public void setup() {
        OnboardingOperations operations = mock(OnboardingOperations.class);
        when(operations.getGenreBuckets()).thenReturn(Observable.from(audio(), music()).cache());

        adapter = new SuggestedUsersAdapter();
        fragment = spy(new SuggestedUsersFragment(operations, adapter));

        SherlockFragmentActivity fragmentActivity = new SherlockFragmentActivity();
        when(fragment.getLayoutInflater(null)).thenReturn(fragmentActivity.getLayoutInflater());
        Robolectric.shadowOf(fragment).setActivity(fragmentActivity);
    }

    @Test
    public void shouldFetchGenreBucketsIntoListAdapterInOnCreate() {
        when(fragment.getListView()).thenReturn(new ListView(Robolectric.application));
        fragment.onViewCreated(new View(Robolectric.application), null);
        expect(adapter.getCount()).toBe(2);
    }

    private GenreBucket audio() {
        Genre audioGenre = new Genre();
        audioGenre.setGrouping(Genre.Grouping.AUDIO);
        return new GenreBucket(audioGenre);
    }

    private GenreBucket music() {
        Genre musicGenre = new Genre();
        musicGenre.setGrouping(Genre.Grouping.MUSIC);
        return new GenreBucket(musicGenre);
    }

}
