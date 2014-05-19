package com.soundcloud.android.explore;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.collections.Section;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.model.ExploreGenresSections;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.view.ListViewController;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rx.Subscription;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ExploreGenresFragmentTest {

    private TestObservables.MockObservable observable;

    @InjectMocks
    private ExploreGenresFragment fragment;
    @Mock
    private ExploreGenresAdapter adapter;
    @Mock
    private ExploreTracksOperations operations;
    @Mock
    private ExploreGenre exploreGenre;
    @Mock
    private ListView listView;
    @Mock
    private ListViewController listViewController;
    @Mock
    private EventBus eventBus;
    @Mock
    private Subscription subscription;

    @Before
    public void setUp() throws Exception {
        Robolectric.shadowOf(fragment).setActivity(new FragmentActivity());
        observable = TestObservables.emptyObservable(subscription);
        when(operations.getCategories()).thenReturn(observable);
    }

    @Test
    public void shouldLoadFirstPageOfTrackSuggestionsWithGenreFromBundleInOnCreate() {
        fragment.onCreate(null);
        expect(observable.subscribedTo()).toBeTrue();
        verify(operations).getCategories();
        verify(adapter).onCompleted();
    }

    @Test
    public void shouldDetachListViewControllerOnDestroyView() {
        fragment.onDestroyView();
        verify(listViewController).onDestroyView();
    }

    @Test
    public void shouldUnsubscribeConnectionSubscriptionInOnDestroy() {
        fragment.onCreate(null);
        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldAddMusicAndAudioSections(){
        ExploreGenre electronicCategory = new ExploreGenre("electronic");
        ExploreGenre comedyCategory = new ExploreGenre("comedy");
        ExploreGenresSections categories = createSectionsFrom(electronicCategory, comedyCategory);
        mockCategoriesObservable(categories);

        createFragment();
        createFragmentView();

        verify(adapter).onNext(buildMusicSection(Lists.newArrayList(electronicCategory)));
        verify(adapter).onNext(buildAudioSection(Lists.newArrayList(comedyCategory)));
    }

    @Test
    public void shouldPublishScreenEnterEventWhenOpeningSpecificGenre(){
        ExploreGenre electronicCategory = new ExploreGenre("electronic");
        ExploreGenre comedyCategory = new ExploreGenre("comedy");
        ExploreGenresSections categories = createSectionsFrom(electronicCategory, comedyCategory);
        mockCategoriesObservable(categories);

        EventMonitor eventMonitor = EventMonitor.on(eventBus);

        fragment.onCreate(null);
        when(listView.getTag()).thenReturn("screentag");
        fragment.onItemClick(listView, listView, 0,0);

        final String screenTag = eventMonitor.verifyEventOn(EventQueue.SCREEN_ENTERED);
        expect(screenTag).toEqual("screentag");
    }

    private void mockCategoriesObservable(ExploreGenresSections categories) {
        observable = TestObservables.just(categories);
        when(operations.getCategories()).thenReturn(observable);
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

    private void createFragment() {
        Robolectric.shadowOf(fragment).setAttached(true);
        fragment.onCreate(null);
    }

    private View createFragmentView() {
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
}
