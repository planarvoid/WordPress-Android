package com.soundcloud.android.explore;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.observables.ConnectableObservable;
import rx.util.functions.Func1;

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
        fragment = new ExploreTracksCategoriesFragment();

        final ExploreTracksCategory electronicCategory = new ExploreTracksCategory("electronic");
        final ExploreTracksCategory comedyCategory = new ExploreTracksCategory("comedy");
        ObjectGraph.create(new TestModule(Observable.just(createSectionsFrom(electronicCategory, comedyCategory)))).inject(fragment);

        View fragmentLayout = createFragmentView();

        final ListView listView = (ListView) fragmentLayout.findViewById(R.id.suggested_tracks_categories_list);
        final ListAdapter adapter = listView.getAdapter();
        expect(adapter.getCount()).toBe(2); // should have 2 sections
        expect(adapter.getItem(0)).toBe(electronicCategory);
        expect(adapter.getItem(1)).toBe(comedyCategory);
    }

    @Test
    public void shouldUnsubscribeFromObservableInOnDestroy() {
        Observable observable = Mockito.mock(Observable.class);
        ConnectableObservable mockObservable = mock(ConnectableObservable.class);

        when(observable.mapMany(any(Func1.class))).thenReturn(observable);
        when(observable.replay()).thenReturn(mockObservable);
        when(mockObservable.observeOn(AndroidSchedulers.mainThread())).thenReturn(mockObservable);

        final Subscription subscription = Mockito.mock(Subscription.class);
        when(mockObservable.connect()).thenReturn(subscription);

        fragment = new ExploreTracksCategoriesFragment();
        ObjectGraph.create(new TestModule(observable)).inject(fragment);

        createFragmentView();
        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldRecreateObservableWhenClickingRetryAfterFailureSoThatWeDontEmitCachedResults() {
        fragment = new ExploreTracksCategoriesFragment();
        final TestModule testModule = new TestModule(Observable.<ExploreTracksCategories>error(new Exception()));
        ObjectGraph.create(testModule).inject(fragment);

        createFragmentView();

        Button retryButton = (Button) fragment.getView().findViewById(R.id.btn_retry);
        expect(retryButton).not.toBeNull();
        retryButton.performClick();

        // this verifies that clicking the retry button does not re-run the initial observable, but a new one.
        // If that wasn't the case, we'd simply replay a failed result.
        expect(testModule.observablesRequested).toEqual(2);
    }

    // HELPERS

    private View createFragmentView() {
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


    @Module(complete = false, injects = {ExploreTracksCategoriesFragment.class}, overrides = true)
    public class TestModule {
        Observable<ExploreTracksCategories> connectableObservable;
        private int observablesRequested = 0;

        public TestModule(Observable<ExploreTracksCategories> connectableObservable) {
            this.connectableObservable = connectableObservable;
        }

        @Provides
        Observable<ExploreTracksCategories> provideCategoryObservable() {
            observablesRequested++;
            return connectableObservable;
        }
    }

}
