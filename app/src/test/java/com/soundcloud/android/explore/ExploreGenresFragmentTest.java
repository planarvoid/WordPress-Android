package com.soundcloud.android.explore;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestFragmentController;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.widget.ListView;

import java.util.List;

public class ExploreGenresFragmentTest extends AndroidUnitTest {

    private PublishSubject<ExploreGenresSections> categoriesOperation;

    private ExploreGenresFragment fragment;
    private TestFragmentController fragmentController;

    private TestEventBus eventBus = new TestEventBus();

    @Mock private ExploreTracksOperations operations;
    @Mock private ExploreGenresAdapter adapter;
    @Mock private ListViewController listViewController;
    @Mock private ExploreGenre exploreGenre;
    @Mock private ListView listView;

    @Before
    public void setUp() throws Exception {
        categoriesOperation = PublishSubject.create();
        when(operations.getCategories()).thenReturn(categoriesOperation);

        fragment = new ExploreGenresFragment(operations, adapter, listViewController, eventBus);
        fragmentController = TestFragmentController.of(fragment);
    }

    @Test
    public void shouldLoadFirstPageOfTrackSuggestionsWithGenreFromBundleInOnCreate() {
        fragmentController.create();

        assertThat(categoriesOperation.hasObservers()).isTrue();
        categoriesOperation.onCompleted();
        verify(adapter).onCompleted();
    }

    @Test
    public void shouldUnsubscribeConnectionSubscriptionInOnDestroy() {
        fragmentController.create();
        fragmentController.destroy();

        assertThat(categoriesOperation.hasObservers()).isFalse();
    }

    @Test
    public void shouldAddMusicAndAudioSections() {
        ExploreGenre electronicCategory = new ExploreGenre("electronic");
        ExploreGenre comedyCategory = new ExploreGenre("comedy");
        ExploreGenresSections categories = createSectionsFrom(electronicCategory, comedyCategory);
        mockCategoriesObservable(categories);

        fragmentController.create();
        fragmentController.createView();

        verify(adapter).onNext(newArrayList(electronicCategory));
        verify(adapter).onNext(newArrayList(comedyCategory));
    }

    @Test
    public void shouldPublishScreenEnterEventWhenOpeningSpecificGenre() {
        when(listView.getTag()).thenReturn("screentag");

        ExploreGenre electronicCategory = new ExploreGenre("electronic");
        ExploreGenre comedyCategory = new ExploreGenre("comedy");
        ExploreGenresSections categories = createSectionsFrom(electronicCategory, comedyCategory);
        mockCategoriesObservable(categories);

        fragmentController.create();
        fragment.onItemClick(listView, listView, 0, 0);

        final ScreenEvent event = (ScreenEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(event.screen()).isEqualTo("screentag");
    }

    private void mockCategoriesObservable(ExploreGenresSections categories) {
        when(operations.getCategories()).thenReturn(Observable.just(categories));
        when(listView.getHeaderViewsCount()).thenReturn(0);
        when(adapter.getItem(0)).thenReturn(categories.getMusic().get(0));
        when(adapter.getItem(1)).thenReturn(categories.getAudio().get(0));
    }

    private ExploreGenresSections createSectionsFrom(ExploreGenre musicCat, ExploreGenre audioCat) {
        final ExploreGenresSections sections = new ExploreGenresSections();
        final List<ExploreGenre> musicCategories = newArrayList(musicCat);
        sections.setMusic(musicCategories);
        final List<ExploreGenre> audioCategories = newArrayList(audioCat);
        sections.setAudio(audioCategories);
        return sections;
    }
}
