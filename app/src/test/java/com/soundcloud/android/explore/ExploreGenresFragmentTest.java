package com.soundcloud.android.explore;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.collections.Section;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.model.ExploreGenresSections;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyListView;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

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
    private ExploreTracksOperations operations;
    @Mock
    private ExploreGenre exploreGenre;
    @Mock
    private ListView listView;
    @Mock
    private ImageOperations imageOperations;
    @Mock
    private EventBus eventBus;

    @Before
    public void setUp() throws Exception {
        fragment = new ExploreGenresFragment(operations, adapter, imageOperations, eventBus);
        Robolectric.shadowOf(fragment).setActivity(new FragmentActivity());
    }

    @Test
    public void shouldAddMusicAndAudioSections(){
        ExploreGenre electronicCategory = new ExploreGenre("electronic");
        ExploreGenre comedyCategory = new ExploreGenre("comedy");
        ExploreGenresSections categories = createSectionsFrom(electronicCategory, comedyCategory);
        addCategoriesToFragment(categories);

        createFragment();
        createFragmentView();

        verify(adapter).onNext(buildMusicSection(Lists.newArrayList(electronicCategory)));
        verify(adapter).onNext(buildAudioSection(Lists.newArrayList(comedyCategory)));
    }

    @Test
    public void shouldRecreateObservableWhenClickingRetryAfterFailureSoThatWeDontEmitCachedResults() {
        when(operations.getCategories()).thenReturn(Observable.<ExploreGenresSections>error(new Exception()));

        createFragment();
        createFragmentView();

        Button retryButton = (Button) fragment.getView().findViewById(R.id.btn_retry);
        expect(retryButton).not.toBeNull();
        retryButton.performClick();

        // this verifies that clicking the retry button does not re-run the initial observable, but a new one.
        // If that wasn't the case, we'd simply replay a failed result.
        verify(operations, times(2)).getCategories();
    }

    @Test
    public void shouldShowProgressSpinnerWhenReloadingResultViaRetryButton() {
        when(operations.getCategories()).thenReturn(Observable.<ExploreGenresSections>error(new Exception()),
                Observable.<ExploreGenresSections>never());

        createFragment();
        final View layout = createFragmentView();

        Button retryButton = (Button) fragment.getView().findViewById(R.id.btn_retry);
        expect(retryButton).not.toBeNull();
        retryButton.performClick();

        EmptyListView emptyView = (EmptyListView) layout.findViewById(android.R.id.empty);
        expect(emptyView.getStatus()).toEqual(EmptyListView.Status.WAITING);
    }

    @Test
    public void shouldNotReloadGenresWhenRecreatingViewsAndGenresAlreadyLoaded() {
        ExploreGenre electronicCategory = new ExploreGenre("electronic");
        ExploreGenre comedyCategory = new ExploreGenre("comedy");
        ExploreGenresSections categories = createSectionsFrom(electronicCategory, comedyCategory);
        addCategoriesToFragment(categories);

        // initial life cycle calls
        createFragment();
        createFragmentView();

        // recreate views, e.g. after getting re-attached to the view pager
        createFragmentView();

        verify(adapter, times(2)).onNext(any(Section.class));
    }

    @Test
    public void shouldPublishScreenEnterEventWhenOpeningSpecificGenre(){
        ExploreGenre electronicCategory = new ExploreGenre("electronic");
        ExploreGenre comedyCategory = new ExploreGenre("comedy");
        ExploreGenresSections categories = createSectionsFrom(electronicCategory, comedyCategory);
        addCategoriesToFragment(categories);

        EventMonitor eventMonitor = EventMonitor.on(eventBus);

        fragment.onCreate(null);
        when(listView.getTag()).thenReturn("screentag");
        fragment.onItemClick(listView, listView, 0,0);

        final String screenTag = eventMonitor.verifyEventOn(EventQueue.SCREEN_ENTERED);
        expect(screenTag).toEqual("screentag");
    }

    private void addCategoriesToFragment(ExploreGenresSections categories) {
        final Observable<ExploreGenresSections> observable = Observable.just(
                categories);
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
