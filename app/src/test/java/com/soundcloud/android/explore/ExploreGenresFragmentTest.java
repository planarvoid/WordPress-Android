package com.soundcloud.android.explore;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.collections.Section;
import com.soundcloud.android.dagger.AndroidObservableFactory;
import com.soundcloud.android.injection.MockInjector;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.model.ExploreGenresSections;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.events.Event;
import com.xtremelabs.robolectric.Robolectric;
import dagger.Module;
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
public class ExploreGenresFragmentTest {

    private ExploreGenresFragment fragment;

    @Mock
    private ExploreGenresAdapter adapter;
    @Mock
    private AndroidObservableFactory factory;
    @Mock
    private Observer<String> screenTrackingObserver;
    @Mock
    private ExploreGenre exploreGenre;
    @Mock
    private Observable observable;
    @Mock
    private ListView listView;
    private MockInjector dependencyInjector;

    @Before
    public void setUp() throws Exception {
        dependencyInjector = new MockInjector(new TestModule(factory));
        fragment = new ExploreGenresFragment(dependencyInjector);
        Robolectric.shadowOf(fragment).setActivity(new FragmentActivity());
    }

    @Test
    public void shouldAddMusicAndAudioSections(){
        ExploreGenre electronicCategory = new ExploreGenre("electronic");
        ExploreGenre comedyCategory = new ExploreGenre("comedy");
        ExploreGenresSections categories = createSectionsFrom(electronicCategory, comedyCategory);
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
        when(factory.create(any(Fragment.class))).thenReturn(Observable.<ExploreGenresSections>error(new Exception()));

        createFragmentView();

        Button retryButton = (Button) fragment.getView().findViewById(R.id.btn_retry);
        expect(retryButton).not.toBeNull();
        retryButton.performClick();

        // this verifies that clicking the retry button does not re-run the initial observable, but a new one.
        // If that wasn't the case, we'd simply replay a failed result.
        verify(factory, times(2)).create(fragment);
    }

    @Test
    public void shouldPublishScreenEnterEventWhenOpeningSpecificGenre(){
        dependencyInjector.inject(fragment);
        Subscription subscription = Event.SCREEN_ENTERED.subscribe(screenTrackingObserver);
        when(listView.getTag()).thenReturn("screentag");
        fragment.onItemClick(listView, listView, 0,0);
        verify(screenTrackingObserver).onNext("screentag");
        verifyNoMoreInteractions(screenTrackingObserver);
        subscription.unsubscribe();
    }

    private void addCategoriesToFragment(ExploreGenresSections categories) {
        final Observable<ExploreGenresSections> observable = Observable.just(
                categories);
        when(factory.create(any(Fragment.class))).thenReturn(observable);
        when(listView.getHeaderViewsCount()).thenReturn(0);
        when(adapter.getSection(0)).thenReturn(buildMusicSection(categories.getMusic()));
        when(adapter.getSection(1)).thenReturn(buildAudioSection(categories.getAudio()));
        when(adapter.getItem(0)).thenReturn(categories.getMusic().get(0));
        when(adapter.getItem(1)).thenReturn(categories.getAudio().get(0));
    }

    private Section<ExploreGenre> buildMusicSection(List<ExploreGenre> categories) {
        return new Section<ExploreGenre>(ExploreGenresAdapter.MUSIC_SECTION,
                R.string.explore_genre_header_music, categories);
    }

    private Section<ExploreGenre> buildAudioSection(List<ExploreGenre> categories) {
        return new Section<ExploreGenre>(ExploreGenresAdapter.AUDIO_SECTION,
                R.string.explore_genre_header_audio, categories);
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

    private ExploreGenresSections createSectionsFrom(ExploreGenre musicCat, ExploreGenre audioCat) {
        final ExploreGenresSections sections = new ExploreGenresSections();
        final ArrayList<ExploreGenre> musicCategories = Lists.newArrayList(musicCat);
        sections.setMusic(musicCategories);
        final ArrayList<ExploreGenre> audioCategories = Lists.newArrayList(audioCat);
        sections.setAudio(audioCategories);
        return sections;
    }

    @Module(injects = ExploreGenresFragment.class)
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
        ExploreGenresAdapter provideExplorePagerAdapter() {
            return adapter;
        }
    }

}
