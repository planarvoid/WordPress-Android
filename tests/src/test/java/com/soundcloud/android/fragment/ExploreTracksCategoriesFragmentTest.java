package com.soundcloud.android.fragment;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ExploreTracksCategoriesAdapter;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.Section;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.RxTestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksCategoriesFragmentTest {

    private ExploreTracksCategoriesFragment fragment;

    @Mock
    private ExploreTracksCategoriesAdapter adapter;

    @Test
    public void shouldAddMusicAndAudioSections(){
        final ExploreTracksCategory electronicCategory = new ExploreTracksCategory("electronic");
        final ExploreTracksCategory comedyCategory = new ExploreTracksCategory("comedy");
        ExploreTracksOperations operations = mock(ExploreTracksOperations.class);
        when(operations.getCategories()).thenReturn(Observable.just(createSectionsFrom(electronicCategory, comedyCategory)));

        fragment = new ExploreTracksCategoriesFragment(operations);
        View fragmentLayout = setupFragment();

        final ListView listView = (ListView) fragmentLayout.findViewById(R.id.suggested_tracks_categories_list);
        final ListAdapter adapter = listView.getAdapter();
        expect(adapter.getCount()).toBe(2); // should have 2 sections
        expect(adapter.getItem(0)).toBe(electronicCategory);
        expect(adapter.getItem(1)).toBe(comedyCategory);
    }

    @Test
    public void shouldUnsubscribeFromObservableInOnDestroy() {
        Subscription subscription = mock(Subscription.class);

        fragment = new ExploreTracksCategoriesFragment(RxTestHelper.<Section<ExploreTracksCategory>>connectableObservableReturning(subscription));
        setupFragment();

        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldRecreateObservableWhenClickingRetryAfterFailureSoThatWeDontEmitCachedResults() {
        ExploreTracksOperations operations = mock(ExploreTracksOperations.class);
        when(operations.getCategories()).thenReturn(Observable.<ExploreTracksCategories>error(new Exception()));

        fragment = new ExploreTracksCategoriesFragment(operations);
        setupFragment();

        Button retryButton = (Button) fragment.getView().findViewById(R.id.btn_retry);
        expect(retryButton).not.toBeNull();
        retryButton.performClick();

        // this verifies that clicking the retry button does not re-run the initial observable, but a new one.
        // If that wasn't the case, we'd simply replay a failed result.
        verify(operations, times(2)).getCategories();
    }

    // HELPERS

    private View setupFragment() {
        Robolectric.shadowOf(fragment).setAttached(true);
        fragment.onCreate(null);

        View fragmentLayout = fragment.onCreateView(LayoutInflater.from(Robolectric.application), new FrameLayout(Robolectric.application), null);
        Robolectric.shadowOf(fragment).setView(fragmentLayout);
        fragment.onViewCreated(fragmentLayout, null);
        return fragmentLayout;
    }

    private ExploreTracksCategories createSectionsFrom(ExploreTracksCategory musicCat, ExploreTracksCategory audioCat) {
        final ExploreTracksCategories sections = new ExploreTracksCategories();
        final ArrayList<ExploreTracksCategory> musicCategories = Lists.newArrayList(musicCat);
        sections.setMusic(musicCategories);
        final ArrayList<ExploreTracksCategory> audioCategories = Lists.newArrayList(audioCat);
        sections.setAudio(audioCategories);
        return sections;
    }
}
