package com.soundcloud.android.explore;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.collections.Section;
import com.soundcloud.android.dagger.AndroidObservableFactory;
import com.soundcloud.android.dagger.DependencyInjector;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.Event;
import com.xtremelabs.robolectric.Robolectric;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.util.functions.Func1;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksCategoriesFragmentTest {

    private ExploreTracksCategoriesFragment fragment;

    @Mock
    private ListView listView;
    @Mock
    private ExploreTracksCategoriesAdapter adapter;
    @Mock
    private AndroidObservableFactory factory;
    @Mock
    private Observer<String> screenTrackingObserver;

    @Before
    public void setUp() throws Exception {
        fragment = new ExploreTracksCategoriesFragment(new DependencyInjector() {
            @Override
            public void inject(Fragment target) {
                ObjectGraph.create(new TestModule(factory)).inject(fragment);
            }

            @Override
            public ObjectGraph fromAppGraphWithModules(Object... modules) {
                return null;
            }
        });
        Robolectric.shadowOf(fragment).setActivity(new FragmentActivity());
    }

    @Test
    public void shouldAddMusicAndAudioSections(){
        final ExploreTracksCategory electronicCategory = new ExploreTracksCategory("electronic");
        final ExploreTracksCategory comedyCategory = new ExploreTracksCategory("comedy");
        ExploreTracksCategories categories = createSectionsFrom(electronicCategory, comedyCategory);
        addCategoriesToFragment(categories);

        createFragmentView();

        verify(adapter).onNext(buildMusicSection(Lists.newArrayList(electronicCategory)));
        verify(adapter).onNext(buildAudioSection(Lists.newArrayList(comedyCategory)));
    }

    @Test
    public void shouldUnsubscribeFromObservableInOnDestroy() {
        Observable observable = Mockito.mock(Observable.class);
        ConnectableObservable mockObservable = mock(ConnectableObservable.class);
        final Subscription subscription = Mockito.mock(Subscription.class);

        when(factory.create(any(Fragment.class))).thenReturn(observable);
        when(observable.mapMany(any(Func1.class))).thenReturn(observable);
        when(observable.replay()).thenReturn(mockObservable);
        when(mockObservable.connect()).thenReturn(subscription);

        createFragmentView();
        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldRecreateObservableWhenClickingRetryAfterFailureSoThatWeDontEmitCachedResults() {
        when(factory.create(any(Fragment.class))).thenReturn(Observable.<ExploreTracksCategories>error(new Exception()));

        createFragmentView();

        Button retryButton = (Button) fragment.getView().findViewById(R.id.btn_retry);
        expect(retryButton).not.toBeNull();
        retryButton.performClick();

        // this verifies that clicking the retry button does not re-run the initial observable, but a new one.
        // If that wasn't the case, we'd simply replay a failed result.
        verify(factory, times(2)).create(fragment);
    }

    @Test
    public void shouldPublishEnterScreenEventInOnResume() {
        Subscription subscription = Event.SCREEN_ENTERED.subscribe(screenTrackingObserver);

        fragment.onResume();
        verify(screenTrackingObserver).onNext(eq("explore:genres"));

        verifyNoMoreInteractions(screenTrackingObserver);
        subscription.unsubscribe();
    }

    @Test
    public void shouldGenerateAndPassCorrectScreenTrackingTagWhenSelectingMusicCategory() {
        ExploreTracksCategories categories = createSectionsFrom(
                new ExploreTracksCategory("electronic"), new ExploreTracksCategory("comedy"));
        addCategoriesToFragment(categories);

        createFragmentView();

        fragment.onItemClick(listView, null, 0, -1);

        Intent firedIntent = Robolectric.shadowOf(fragment.getActivity()).getNextStartedActivity();
        expect(firedIntent).not.toBeNull();
        expect(firedIntent.getStringExtra(ExploreTracksFragment.SCREEN_TRACKING_TAG_EXTRA)).toEqual(
                "explore:genres:music:electronic");
    }

    @Test
    public void shouldGenerateAndPassCorrectScreenTrackingTagWhenSelectingAudioCategory() {
        ExploreTracksCategories categories = createSectionsFrom(
                new ExploreTracksCategory("electronic"), new ExploreTracksCategory("Religion & Spirituality"));
        addCategoriesToFragment(categories);

        createFragmentView();

        fragment.onItemClick(listView, null, 1, -1);

        Intent firedIntent = Robolectric.shadowOf(fragment.getActivity()).getNextStartedActivity();
        expect(firedIntent).not.toBeNull();
        expect(firedIntent.getStringExtra(ExploreTracksFragment.SCREEN_TRACKING_TAG_EXTRA)).toEqual(
                "explore:genres:audio:religion_&_spirituality");
    }

    private void addCategoriesToFragment(ExploreTracksCategories categories) {
        final Observable<ExploreTracksCategories> observable = Observable.just(
                categories);
        when(factory.create(any(Fragment.class))).thenReturn(observable);
        when(listView.getHeaderViewsCount()).thenReturn(0);
        when(adapter.getSection(0)).thenReturn(buildMusicSection(categories.getMusic()));
        when(adapter.getSection(1)).thenReturn(buildAudioSection(categories.getAudio()));
        when(adapter.getItem(0)).thenReturn(categories.getMusic().get(0));
        when(adapter.getItem(1)).thenReturn(categories.getAudio().get(0));
    }

    private Section<ExploreTracksCategory> buildMusicSection(List<ExploreTracksCategory> categories) {
        return new Section<ExploreTracksCategory>(ExploreTracksCategoriesAdapter.MUSIC_SECTION,
                R.string.explore_category_header_music, categories);
    }

    private Section<ExploreTracksCategory> buildAudioSection(List<ExploreTracksCategory> categories) {
        return new Section<ExploreTracksCategory>(ExploreTracksCategoriesAdapter.AUDIO_SECTION,
                R.string.explore_category_header_audio, categories);
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

    @Module(injects = ExploreTracksCategoriesFragment.class)
    public class TestModule {
        AndroidObservableFactory observableFactory;

        public TestModule(AndroidObservableFactory observableFactory) {
            this.observableFactory = observableFactory;
        }

        @Provides
        AndroidObservableFactory provideFactory() {
            return factory;
        }

        @Provides
        ExploreTracksCategoriesAdapter provideExplorePagerAdapter() {
            return adapter;
        }
    }

}
