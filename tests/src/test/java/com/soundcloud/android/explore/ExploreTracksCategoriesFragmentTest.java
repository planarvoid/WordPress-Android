package com.soundcloud.android.explore;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.dagger.AndroidObservableFactory;
import com.soundcloud.android.dagger.DependencyInjector;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.collections.Section;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
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
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.util.functions.Func1;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.util.ArrayList;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksCategoriesFragmentTest {

    private ExploreTracksCategoriesFragment fragment;

    @Mock
    private ExploreTracksCategoriesAdapter adapter;
    @Mock
    private AndroidObservableFactory factory;

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
    }

    @Test
    public void shouldAddMusicAndAudioSections(){
        final ExploreTracksCategory electronicCategory = new ExploreTracksCategory("electronic");
        final ExploreTracksCategory comedyCategory = new ExploreTracksCategory("comedy");
        ExploreTracksCategories sections = createSectionsFrom(electronicCategory, comedyCategory);
        final Observable<ExploreTracksCategories> observable = Observable.just(sections);
        when(factory.create(any(Fragment.class))).thenReturn(observable);

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
